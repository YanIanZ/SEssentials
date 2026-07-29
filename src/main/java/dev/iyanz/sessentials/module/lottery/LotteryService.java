package dev.iyanz.sessentials.module.lottery;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.economy.EconomyHook;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.SmallCaps;
import dev.iyanz.sessentials.util.Style;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * In-memory state and draw logic for the lottery.
 *
 * <p>The {@code pool} is a flat list of buyer UUIDs — one entry per purchased ticket,
 * so a player's odds scale with how many tickets they hold — and the {@code pot}
 * accumulates the ticket revenue. A random winner is drawn with
 * {@link ThreadLocalRandom} over that list, the whole pot is deposited to them, and
 * both the pool and the pot reset. Neither is persisted: they start empty on every
 * server boot (the info command tells players as much).</p>
 *
 * <p>Thread-safety: purchases arrive on region/command threads while the scheduled
 * draw fires on the async scheduler, so every method that touches the pool or pot is
 * {@code synchronized} on this instance. The next-draw epoch is {@code volatile} for
 * lock-free countdown reads.</p>
 */
final class LotteryService {

    private final SEssentialsPlugin plugin;
    private final EconomyHook eco;
    private final long intervalSeconds;

    private final List<UUID> pool = new ArrayList<>();
    private double pot;
    private volatile long nextDrawEpochSeconds;

    /**
     * @param plugin          the owning plugin (for scheduler + broadcast hops)
     * @param eco             the economy hook used for the winner deposit
     * @param intervalSeconds seconds between automatic draws (used for the countdown)
     */
    LotteryService(SEssentialsPlugin plugin, EconomyHook eco, long intervalSeconds) {
        this.plugin = plugin;
        this.eco = eco;
        this.intervalSeconds = intervalSeconds;
        this.nextDrawEpochSeconds = Instant.now().getEpochSecond() + intervalSeconds;
    }

    /**
     * Records a completed ticket purchase: adds {@code count} entries for the buyer and
     * grows the pot by {@code amountPaid}. The caller must have already withdrawn the
     * money from the buyer.
     *
     * @param buyer      the purchasing player's UUID
     * @param count      number of tickets bought (each becomes one pool entry)
     * @param amountPaid total money paid, added to the pot
     * @return the buyer's new ticket count
     */
    synchronized int recordPurchase(UUID buyer, int count, double amountPaid) {
        for (int i = 0; i < count; i++) {
            pool.add(buyer);
        }
        pot += amountPaid;
        return ticketsOf(buyer);
    }

    /** @return the current pot. */
    synchronized double pot() {
        return pot;
    }

    /** @return the total number of tickets in the pool. */
    synchronized int totalTickets() {
        return pool.size();
    }

    /**
     * @param player a player's UUID
     * @return how many tickets that player currently holds
     */
    synchronized int ticketsOf(UUID player) {
        int held = 0;
        for (UUID entry : pool) {
            if (entry.equals(player)) {
                held++;
            }
        }
        return held;
    }

    /** @return whole seconds until the next scheduled draw (never negative). */
    long secondsUntilDraw() {
        return Math.max(0L, nextDrawEpochSeconds - Instant.now().getEpochSecond());
    }

    /**
     * Runs a draw now: if the pool holds entries, a random winner is paid the whole pot
     * and both reset; otherwise the draw rolls over with an announcement. Either way the
     * next-draw countdown is advanced by one interval. Invoked both by the repeating
     * scheduler and by the manual admin command.
     */
    synchronized void runDraw() {
        nextDrawEpochSeconds = Instant.now().getEpochSecond() + intervalSeconds;
        if (pool.isEmpty()) {
            broadcast(Style.GRAY + SmallCaps.of("No lottery entries this draw — rolling over."));
            return;
        }
        UUID winner = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        double winnings = pot;
        if (!eco.deposit(winner, winnings)) {
            // Payout failed (e.g. winner at the provider's max-money cap). Do NOT clear the
            // pool/pot — keep them so the whole pot rolls over to the next draw instead of
            // being destroyed.
            plugin.getLogger().warning("Lottery payout of " + eco.format(winnings) + " to " + winner
                    + " failed; keeping the pot for the next draw.");
            broadcast(Style.GRAY + SmallCaps.of("Lottery payout failed — the pot rolls over to the next draw."));
            return;
        }
        pool.clear();
        pot = 0.0;
        broadcast(Style.INFO + "<bold>" + SmallCaps.of("Lottery") + "</bold> "
                + Style.GRAY + SmallCaps.of("winner: ") + Style.VALUE + nameOf(winner) + " "
                + Style.GRAY + SmallCaps.of("takes the pot of ") + Style.COIN + eco.format(winnings) + "<gray>!");
    }

    /** Resolves a display name for a winner UUID, preferring an online lookup. */
    private String nameOf(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        String offline = Bukkit.getOfflinePlayer(uuid).getName();
        return offline != null ? offline : uuid.toString();
    }

    /**
     * Sends a prefixed MiniMessage line to the console and every online player, hopping
     * to each player's own region thread (Folia-safe messaging).
     */
    private void broadcast(String miniMessageBody) {
        Component line = Msg.mm(Msg.prefix() + miniMessageBody);
        Bukkit.getConsoleSender().sendMessage(line);
        for (Player player : Bukkit.getOnlinePlayers()) {
            Schedulers.entity(plugin, player, () -> player.sendMessage(line));
        }
    }

    /**
     * Formats a whole-second duration as a compact {@code 1h 5m 30s} string.
     *
     * @param totalSeconds seconds remaining
     * @return a human-readable countdown, or {@code "now"} when non-positive
     */
    static String formatDuration(long totalSeconds) {
        if (totalSeconds <= 0) {
            return "now";
        }
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (hours > 0 || minutes > 0) {
            sb.append(minutes).append("m ");
        }
        sb.append(seconds).append('s');
        return sb.toString();
    }
}
