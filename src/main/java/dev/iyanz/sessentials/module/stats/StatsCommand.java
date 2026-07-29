package dev.iyanz.sessentials.module.stats;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Registers {@code /stats [player]}: reports the sender's own key statistics, or an
 * online player's with the {@code sessentials.stats.others} permission. Every value
 * is read from Bukkit's built-in {@link Statistic} tracking rather than custom
 * bookkeeping, so the target must currently be online for the statistics to be
 * readable.
 */
@SuppressWarnings("UnstableApiUsage")
final class StatsCommand {

    private StatsCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            var node = Commands.literal("stats")
                    .requires(s -> s.getSender().hasPermission("sessentials.stats"))
                    .executes(Cmds.playerExec(StatsCommand::reportSelf))
                    .then(Commands.argument("player", StringArgumentType.word())
                            .suggests(Cmds.PLAYERS)
                            .requires(s -> s.getSender().hasPermission("sessentials.stats.others"))
                            .executes(ctx -> reportOther(plugin, ctx)))
                    .build();
            reg.register(node, "Show your or another player's statistics");
        });
    }

    /** Reports the sender's own statistics; already runs on the sender's region thread. */
    private static void reportSelf(Player player) {
        report(player, player, "Your statistics:");
    }

    /**
     * Resolves the {@code player} argument to an online target and reports their
     * statistics to the sender, reading them on the target's region thread (Folia-safe)
     * since statistics live on the target's live entity data.
     */
    private static int reportOther(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Msg.err(sender, name + " must be online to check statistics.");
            return 0;
        }
        Schedulers.entity(plugin, target, () -> report(sender, target, target.getName() + "'s statistics:"));
        return 1;
    }

    /**
     * Sends the full statistics block for {@code target} to {@code recipient}: a
     * heading followed by one {@link Msg#value} line per tracked statistic.
     *
     * @param recipient  who receives the report
     * @param target     whose statistics are read (must be online)
     * @param headerText the plain-text heading line (small-capped like the rest)
     */
    private static void report(CommandSender recipient, Player target, String headerText) {
        Msg.info(recipient, headerText);
        Msg.value(recipient, "Deaths:", String.valueOf(target.getStatistic(Statistic.DEATHS)));
        Msg.value(recipient, "Player kills:", String.valueOf(target.getStatistic(Statistic.PLAYER_KILLS)));
        Msg.value(recipient, "Mob kills:", String.valueOf(target.getStatistic(Statistic.MOB_KILLS)));
        Msg.value(recipient, "Damage dealt:", StatsFormat.damage(target.getStatistic(Statistic.DAMAGE_DEALT)));
        Msg.value(recipient, "Damage taken:", StatsFormat.damage(target.getStatistic(Statistic.DAMAGE_TAKEN)));
        Msg.value(recipient, "Jumps:", String.valueOf(target.getStatistic(Statistic.JUMP)));
        Msg.value(recipient, "Play time:", StatsFormat.duration(target.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L));
        Msg.value(recipient, "Distance travelled:", StatsFormat.distance(distanceCentimetres(target)));
    }

    /** @return the target's total walked + sprinted distance, in centimetres. */
    private static long distanceCentimetres(Player target) {
        return (long) target.getStatistic(Statistic.WALK_ONE_CM) + (long) target.getStatistic(Statistic.SPRINT_ONE_CM);
    }
}
