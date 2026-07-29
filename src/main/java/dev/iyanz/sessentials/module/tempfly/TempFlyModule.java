package dev.iyanz.sessentials.module.tempfly;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Temporary-flight command module. {@code /tempfly <seconds>} grants the sender
 * temporary flight for a bounded number of seconds, and {@code /tempfly <player>
 * <seconds>} grants it to another online player.
 *
 * <p>All the flight state (enabling flight, the auto-revoke timer, and cleaning up on
 * quit) lives in {@link TempFlyGrants}; this class only shapes and registers the
 * Brigadier command and wires the {@link TempFlyQuitListener}.</p>
 *
 * <p>The bare {@code tempfly} literal carries no permission of its own so that the
 * {@code seconds} branch (gated by {@code sessentials.tempfly}) and the {@code player}
 * branch (gated by {@code sessentials.tempfly.others}) stay independent — Brigadier
 * gates an entire subtree on an ancestor's {@code requires()}, so gating the shared
 * literal would force the "others" variant to also need the self permission. The
 * integer {@code seconds} branch is registered before the {@code player} branch so a
 * bare numeric argument (e.g. {@code /tempfly 60}) resolves to the self variant rather
 * than being read as a player name.</p>
 *
 * <p>Folia-safe: the actual flight mutation and the auto-revoke run on the target's own
 * region thread (see {@link TempFlyGrants}).</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class TempFlyModule implements EssModule {

    /** Smallest accepted grant duration, in seconds. */
    private static final int MIN_SECONDS = 1;

    /** Largest accepted grant duration, in seconds (24 hours). */
    private static final int MAX_SECONDS = 86_400;

    /** Owns the active grants and their auto-revoke tasks. */
    private final TempFlyGrants grants = new TempFlyGrants();

    @Override
    public String name() {
        return "tempfly";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new TempFlyQuitListener(grants), plugin);
        plugin.commands(reg -> reg.register(Commands.literal("tempfly")
                .then(Commands.argument("seconds", IntegerArgumentType.integer(MIN_SECONDS, MAX_SECONDS))
                        .requires(s -> s.getSender().hasPermission("sessentials.tempfly"))
                        .executes(ctx -> {
                            Player self = Cmds.player(ctx);
                            if (self == null) {
                                return 0;
                            }
                            grants.grant(plugin, self, self, IntegerArgumentType.getInteger(ctx, "seconds"));
                            return 1;
                        }))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(Cmds.PLAYERS)
                        .requires(s -> s.getSender().hasPermission("sessentials.tempfly.others"))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(MIN_SECONDS, MAX_SECONDS))
                                .executes(ctx -> grantOther(plugin, ctx))))
                .build(), "Grant yourself or a player temporary flight for a number of seconds"));
    }

    @Override
    public void disable(SEssentialsPlugin plugin) {
        grants.cancelAll();
    }

    /**
     * Resolves the online target from the {@code player} argument and delegates the
     * grant; the effect itself is applied on the target's region thread.
     *
     * @param plugin the owning plugin, used to hop to the target's region thread
     * @param ctx    the command context holding the {@code player} and {@code seconds} arguments
     * @return {@code 1} if the target was online and the grant was scheduled, {@code 0} otherwise
     */
    private int grantOther(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Msg.err(sender, name + " is not online.");
            return 0;
        }
        grants.grant(plugin, sender, target, IntegerArgumentType.getInteger(ctx, "seconds"));
        return 1;
    }
}
