package dev.iyanz.sessentials.module.playtime;

import java.util.List;

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
 * Registers {@code /playtime} (alias {@code /pt}): reports the sender's own total play
 * time, or an online player's with the {@code sessentials.playtime.others} permission.
 * Play time is derived from Bukkit's built-in {@link Statistic#PLAY_ONE_MINUTE}
 * statistic (ticks played, 20 per second) rather than custom bookkeeping, so the
 * target must currently be online for the statistic to be readable.
 */
@SuppressWarnings("UnstableApiUsage")
final class PlaytimeCommand {

    private PlaytimeCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            var node = Commands.literal("playtime")
                    .requires(s -> s.getSender().hasPermission("sessentials.playtime"))
                    .executes(Cmds.playerExec(PlaytimeCommand::reportSelf))
                    .then(Commands.argument("player", StringArgumentType.word())
                            .suggests(Cmds.PLAYERS)
                            .requires(s -> s.getSender().hasPermission("sessentials.playtime.others"))
                            .executes(ctx -> reportOther(plugin, ctx)))
                    .build();
            reg.register(node, "Show total play time", List.of("pt"));
        });
    }

    private static void reportSelf(Player player) {
        Msg.value(player, "Your play time:", PlaytimeFormat.format(ticksToSeconds(player)));
    }

    private static int reportOther(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Msg.err(sender, name + " must be online to check play time.");
            return 0;
        }
        // The statistic lives on the target's live entity data — read it on their
        // region thread (Folia-safe) rather than the sender's.
        Schedulers.entity(plugin, target, () ->
                Msg.value(sender, target.getName() + "'s play time:", PlaytimeFormat.format(ticksToSeconds(target))));
        return 1;
    }

    private static long ticksToSeconds(Player player) {
        return player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L;
    }
}
