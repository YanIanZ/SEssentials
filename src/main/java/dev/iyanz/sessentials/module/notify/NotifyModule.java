package dev.iyanz.sessentials.module.notify;

import com.mojang.brigadier.arguments.StringArgumentType;
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
 * Staff-facing notifications: a manual {@code /notify <text>} broadcast, plus an
 * optional automatic join/leave awareness feed handled by {@link NotifyListener}.
 * Both are delivered only to online players holding {@link #RECEIVE_PERMISSION}.
 *
 * <p>The join/leave feed is off by default and gated by the {@code notify.join-leave}
 * config key; a server operator opts in by setting it to {@code true}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class NotifyModule implements EssModule {

    /** Permission required to receive both the manual and join/leave notifications. */
    static final String RECEIVE_PERMISSION = "sessentials.notify.receive";

    @Override
    public String name() {
        return "notify";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("notify")
                .requires(s -> s.getSender().hasPermission("sessentials.notify"))
                .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(ctx -> handleNotify(plugin, ctx)))
                .build(), "Send a one-off notification to staff"));

        plugin.getServer().getPluginManager().registerEvents(new NotifyListener(plugin), plugin);
    }

    private static int handleNotify(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        String text = StringArgumentType.getString(ctx, "text");
        Component message = label().append(Msg.mm(text));
        broadcast(plugin, message);
        Msg.ok(ctx.getSource().getSender(), "Notification sent.");
        return 1;
    }

    /**
     * Sends {@code message} to the console and to every online holder of
     * {@link #RECEIVE_PERMISSION}, each on its own region thread (Folia-safe).
     *
     * @param plugin  the owning plugin, used for the region-thread hop
     * @param message the pre-formatted message to deliver
     */
    static void broadcast(SEssentialsPlugin plugin, Component message) {
        Bukkit.getConsoleSender().sendMessage(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission(RECEIVE_PERMISSION)) {
                continue;
            }
            Schedulers.entity(plugin, player, () -> player.sendMessage(message));
        }
    }

    /** {@code [Notify] } label in the shared info colour, shared by the command and the join/leave feed. */
    static Component label() {
        return Msg.mm(Style.GRAY + "[" + Style.INFO + SmallCaps.of("Notify") + Style.GRAY + "] ");
    }
}
