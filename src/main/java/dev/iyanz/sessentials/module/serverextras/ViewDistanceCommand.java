package dev.iyanz.sessentials.module.serverextras;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Registers {@code /viewdistance <n> [player]}: sets a player's per-player send view
 * distance through Paper's {@link Player#setSendViewDistance(int)} — the number of
 * chunks actually sent to that client, independent of the server-wide setting.
 *
 * <p>The distance is constrained to {@code 2..32} by the argument parser. Without a
 * target the sender's own distance is changed; naming a target requires the
 * {@code sessentials.viewdistance.others} permission. The setter runs on the target's
 * region thread (Folia-safe).</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class ViewDistanceCommand {

    /** Permission to change your own send view distance; {@code .others} for targets. */
    private static final String PERMISSION = "sessentials.viewdistance";

    /** Lowest send view distance the client protocol meaningfully supports. */
    private static final int MIN_DISTANCE = 2;

    /** Highest send view distance Paper accepts. */
    private static final int MAX_DISTANCE = 32;

    private ViewDistanceCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("viewdistance")
                .requires(s -> s.getSender().hasPermission(PERMISSION))
                .then(Commands.argument("chunks", IntegerArgumentType.integer(MIN_DISTANCE, MAX_DISTANCE))
                        .executes(ctx -> applyToSelf(plugin, ctx))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(Cmds.PLAYERS)
                                .requires(s -> s.getSender().hasPermission(PERMISSION + ".others"))
                                .executes(ctx -> applyToTarget(plugin, ctx))))
                .build(), "Set a player's send view distance"));
    }

    /** Applies the distance to the sender themselves (player-only path). */
    private static int applyToSelf(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        Player self = Cmds.player(ctx);
        if (self == null) {
            return 0;
        }
        apply(plugin, ctx.getSource().getSender(), self, IntegerArgumentType.getInteger(ctx, "chunks"));
        return 1;
    }

    /** Applies the distance to the named online player. */
    private static int applyToTarget(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Msg.err(ctx.getSource().getSender(), name + " is not online.");
            return 0;
        }
        apply(plugin, ctx.getSource().getSender(), target, IntegerArgumentType.getInteger(ctx, "chunks"));
        return 1;
    }

    /**
     * Sets the send view distance on the target's region thread and reports back.
     *
     * @param plugin the owning plugin (for scheduler access)
     * @param sender the command sender to notify
     * @param target the player whose send view distance changes
     * @param chunks the new distance in chunks (already constrained by the parser)
     */
    private static void apply(SEssentialsPlugin plugin, CommandSender sender, Player target, int chunks) {
        int distance = Math.clamp(chunks, MIN_DISTANCE, MAX_DISTANCE);
        Schedulers.entity(plugin, target, () -> {
            target.setSendViewDistance(distance);
            Msg.value(target, "Your view distance is now:", distance + " chunks");
        });
        if (!target.equals(sender)) {
            Msg.ok(sender, "Set " + target.getName() + "'s view distance to " + distance + " chunks.");
        }
    }
}
