package dev.iyanz.sessentials.module.staffextras;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * The two staff toggle commands.
 *
 * <ul>
 *   <li>{@code /shadowmute <player>} ({@code sessentials.shadowmute}) — flips the
 *       target's shadow mute. Only the staff member is told; the target is never
 *       notified, which is the whole point of a shadow mute.</li>
 *   <li>{@code /notarget} ({@code sessentials.notarget}) — mobs ignore the sender;
 *       {@code /notarget <player>} ({@code sessentials.notarget.others}) toggles it
 *       for someone else.</li>
 * </ul>
 *
 * <p>Folia-safe: both toggles only mutate concurrent sets in
 * {@link StaffExtrasState} and send thread-safe messages — no entity or world API is
 * touched, so no region hop is needed.</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class ToggleCommands {

    private ToggleCommands() {
    }

    /**
     * Registers {@code /shadowmute} and {@code /notarget}.
     *
     * @param plugin the owning plugin
     * @param state  the shared module state the toggles flip
     */
    static void register(SEssentialsPlugin plugin, StaffExtrasState state) {
        plugin.commands(reg -> reg.register(Commands.literal("shadowmute")
                .requires(s -> s.getSender().hasPermission("sessentials.shadowmute"))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(Cmds.PLAYERS)
                        .executes(ctx -> shadowMute(state, ctx)))
                .build(), "Toggle a silent mute the target cannot detect"));

        plugin.commands(reg -> reg.register(Commands.literal("notarget")
                .requires(s -> s.getSender().hasPermission("sessentials.notarget"))
                .executes(Cmds.playerExec(self -> noTargetSelf(state, self)))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(Cmds.PLAYERS)
                        .requires(s -> s.getSender().hasPermission("sessentials.notarget.others"))
                        .executes(ctx -> noTargetOther(state, ctx)))
                .build(), "Toggle whether mobs ignore you or a player"));
    }

    /** Flips the target's shadow mute and reports the new state to the staff member only. */
    private static int shadowMute(StaffExtrasState state, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        Player target = onlineTarget(ctx, sender);
        if (target == null) {
            return 0;
        }
        boolean muted = state.toggleShadowMute(target.getUniqueId());
        if (muted) {
            Msg.ok(sender, target.getName() + " is now shadow-muted; only they see their own chat.");
        } else {
            Msg.ok(sender, target.getName() + " is no longer shadow-muted.");
        }
        return 1;
    }

    /** Flips the sender's own no-target flag. */
    private static void noTargetSelf(StaffExtrasState state, Player self) {
        boolean ignored = state.toggleNoTarget(self.getUniqueId());
        if (ignored) {
            Msg.ok(self, "Mobs now ignore you.");
        } else {
            Msg.ok(self, "Mobs will target you again.");
        }
    }

    /** Flips another player's no-target flag and tells both sides. */
    private static int noTargetOther(StaffExtrasState state, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        Player target = onlineTarget(ctx, sender);
        if (target == null) {
            return 0;
        }
        boolean ignored = state.toggleNoTarget(target.getUniqueId());
        if (ignored) {
            Msg.ok(sender, "Mobs now ignore " + target.getName() + ".");
            if (!target.equals(sender)) {
                Msg.info(target, "Mobs now ignore you.");
            }
        } else {
            Msg.ok(sender, "Mobs will target " + target.getName() + " again.");
            if (!target.equals(sender)) {
                Msg.info(target, "Mobs will target you again.");
            }
        }
        return 1;
    }

    /**
     * Resolves the {@code player} argument to an online player, erroring the sender
     * on a miss.
     *
     * @param ctx    the command context holding the {@code player} argument
     * @param sender the sender to message on failure
     * @return the online target, or {@code null} if they are not online
     */
    private static Player onlineTarget(CommandContext<CommandSourceStack> ctx, CommandSender sender) {
        String name = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Msg.err(sender, name + " is not online.");
        }
        return target;
    }
}
