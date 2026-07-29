package dev.iyanz.sessentials.module.slowmode;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.SmallCaps;
import dev.iyanz.sessentials.util.Style;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Global chat slowmode. Staff set a single server-wide gap with {@code /slowmode <seconds>}
 * ({@code 0} turns it off); every non-exempt player must then wait that many seconds between
 * chat messages. The gap is held in a {@link SlowModeState} and enforced by
 * {@link SlowModeListener}; this class wires the listener in and exposes the command.
 *
 * <p>Setting the value broadcasts the change to the whole server (console + every online
 * player). The command is gated behind {@code sessentials.slowmode}; the matching
 * {@code sessentials.slowmode.bypass} exempts a player from the limit.</p>
 *
 * <p>Folia-safe: the command only mutates the concurrent-safe holder, and the broadcast
 * fans out to each player on its own region thread.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class SlowModeModule implements EssModule {

    /** Permission required to change the global slowmode. */
    private static final String PERMISSION = "sessentials.slowmode";

    /** Shared holder that the command writes and the listener reads. */
    private final SlowModeState state = new SlowModeState();

    @Override
    public String name() {
        return "slowmode";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new SlowModeListener(state), plugin);
        plugin.commands(reg -> reg.register(Commands.literal("slowmode")
                .requires(s -> s.getSender().hasPermission(PERMISSION))
                .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                        .executes(ctx -> set(plugin, ctx)))
                .build(), "Set the global chat slowmode (0 = off)"));
    }

    /** Applies the new slowmode value and announces it server-wide. */
    private int set(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
        state.seconds(seconds);

        Component prefix = Msg.mm(Style.GRAY + "[" + Style.INFO + SmallCaps.of("Slowmode") + Style.GRAY + "] ");
        String bodyText = seconds > 0
                ? "Chat slowmode set to " + seconds + "s."
                : "Chat slowmode disabled.";
        Component message = prefix.append(Msg.mm(Style.INFO + SmallCaps.of(bodyText)));
        broadcast(plugin, message);
        return 1;
    }

    /** Sends {@code message} to the console and to every online player, each on its own region thread. */
    private static void broadcast(SEssentialsPlugin plugin, Component message) {
        Bukkit.getConsoleSender().sendMessage(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            Schedulers.entity(plugin, player, () -> player.sendMessage(message));
        }
    }
}
