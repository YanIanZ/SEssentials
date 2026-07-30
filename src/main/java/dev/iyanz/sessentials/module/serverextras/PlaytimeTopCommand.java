package dev.iyanz.sessentials.module.serverextras;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;

/**
 * Registers {@code /playtimetop}: a ranked leaderboard of total play time across every
 * player that has ever joined.
 *
 * <p>The {@code playtime} module keeps no store of its own — it reports the built-in
 * {@link Statistic#PLAY_ONE_MINUTE} statistic (ticks played, 20 per second), which the
 * server itself persists per player in the world's stats files. This command reads
 * that exact same data source, via {@link OfflinePlayer#getStatistic(Statistic)}, so
 * the leaderboard always agrees with what {@code /playtime} reports — strictly
 * read-only, no bookkeeping duplicated.</p>
 *
 * <p>Reading every known player's statistic touches one stats file per player, so the
 * aggregation runs on the async scheduler (Folia-safe: no entity or world state is
 * mutated) and the ranked list is messaged back when it completes.</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class PlaytimeTopCommand {

    /** Permission required to view the leaderboard. */
    private static final String PERMISSION = "sessentials.playtimetop";

    /** Number of entries shown on the leaderboard. */
    private static final int TOP_SIZE = 10;

    private static final long TICKS_PER_SECOND = 20L;
    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long SECONDS_PER_HOUR = 60L * SECONDS_PER_MINUTE;
    private static final long SECONDS_PER_DAY = 24L * SECONDS_PER_HOUR;

    /** A single leaderboard row: who, and how many ticks they have played. */
    private record Entry(String name, long ticks) {
    }

    private PlaytimeTopCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("playtimetop")
                .requires(s -> s.getSender().hasPermission(PERMISSION))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    Msg.info(sender, "Crunching play time, one moment...");
                    Schedulers.async(plugin, task -> report(sender));
                    return 1;
                })
                .build(), "Show the play time leaderboard"));
    }

    /**
     * Aggregates every known player's persisted play-time statistic, ranks the top
     * {@value #TOP_SIZE} and messages the list to {@code sender}. Runs off-thread;
     * messaging is thread-safe on Paper (Adventure audiences).
     *
     * @param sender the command sender to receive the leaderboard
     */
    private static void report(CommandSender sender) {
        List<Entry> entries = new ArrayList<>();
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            long ticks;
            try {
                ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
            } catch (IllegalArgumentException e) {
                continue; // unreadable/corrupt stats entry — skip this player
            }
            if (ticks <= 0L) {
                continue;
            }
            String name = player.getName();
            entries.add(new Entry(name != null ? name : player.getUniqueId().toString(), ticks));
        }
        if (entries.isEmpty()) {
            Msg.err(sender, "No play time recorded yet.");
            return;
        }
        entries.sort(Comparator.comparingLong(Entry::ticks).reversed());

        int shown = Math.min(TOP_SIZE, entries.size());
        Msg.info(sender, "Play time leaderboard (top " + shown + "):");
        for (int i = 0; i < shown; i++) {
            Entry entry = entries.get(i);
            Msg.value(sender, "#" + (i + 1) + " " + entry.name() + ":", format(entry.ticks() / TICKS_PER_SECOND));
        }
    }

    /**
     * Renders a whole-seconds duration as a short {@code "Xd Yh Zm"} string, omitting
     * leading zero units but keeping every unit below the first one printed.
     *
     * @param totalSeconds elapsed play time in whole seconds (negatives treated as zero)
     * @return the human-readable rendering
     */
    private static String format(long totalSeconds) {
        long seconds = Math.max(0L, totalSeconds);
        long days = seconds / SECONDS_PER_DAY;
        long hours = (seconds % SECONDS_PER_DAY) / SECONDS_PER_HOUR;
        long minutes = (seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE;

        StringBuilder out = new StringBuilder();
        if (days > 0L) {
            out.append(days).append("d ");
        }
        if (days > 0L || hours > 0L) {
            out.append(hours).append("h ");
        }
        return out.append(minutes).append("m").toString();
    }
}
