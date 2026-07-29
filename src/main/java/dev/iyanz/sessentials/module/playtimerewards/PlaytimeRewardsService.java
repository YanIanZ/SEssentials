package dev.iyanz.sessentials.module.playtimerewards;

import java.util.ArrayList;
import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.store.YamlStore;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

/**
 * Checks a single player's total playtime against the configured reward milestones
 * and grants any newly-unlocked, not-yet-claimed rewards.
 *
 * <p>{@link org.bukkit.Statistic#PLAY_ONE_MINUTE} lives on the player's live entity
 * data, so callers must already be running on that player's region thread before
 * calling {@link #check}: the module's repeating task hops there via
 * {@link dev.iyanz.sessentials.scheduler.Schedulers#entity}, and a player invoking
 * {@code /playtimerewards check} on themselves is already on their own region thread
 * inside the command executor.</p>
 *
 * <p>Claimed rewards are tracked per player in the {@code "playtimerewards"} data
 * store as a list of reward ids under {@code "<uuid>.claimed"}, checked before every
 * grant so a reward is never handed out twice.</p>
 */
final class PlaytimeRewardsService {

    private static final String STORE_NAME = "playtimerewards";
    private static final String CLAIMED_SUFFIX = ".claimed";
    private static final long TICKS_PER_SECOND = 20L;
    private static final long SECONDS_PER_MINUTE = 60L;
    private static final String PLAYER_PLACEHOLDER = "%player%";

    private PlaytimeRewardsService() {
    }

    /**
     * Grants every unlocked, unclaimed reward to {@code player}.
     *
     * @param plugin   the owning plugin
     * @param settings the current reward configuration
     * @param player   the player to check; the caller must already be on their region thread
     */
    static void check(SEssentialsPlugin plugin, PlaytimeRewardsSettings settings, Player player) {
        List<RewardDefinition> rewards = settings.rewards();
        if (rewards.isEmpty()) {
            return;
        }
        long playMinutes = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / TICKS_PER_SECOND / SECONDS_PER_MINUTE;

        YamlStore store = plugin.stores().get(STORE_NAME);
        String claimedPath = player.getUniqueId() + CLAIMED_SUFFIX;
        // Read via the monitor-guarded typed accessor (never the raw config()): several
        // players' region threads run check() in parallel against the shared store, so a
        // raw config() read/write would race (CME / lost update / dupe payout).
        List<String> claimed = new ArrayList<>(store.getStringList(claimedPath));

        for (RewardDefinition reward : rewards) {
            if (reward.minutes() > playMinutes || claimed.contains(reward.id())) {
                continue;
            }
            // Record and PERSIST the claim before dispatching the reward, so a re-entrant
            // check can never observe this milestone as unclaimed and double-grant it.
            claimed.add(reward.id());
            store.set(claimedPath, new ArrayList<>(claimed));
            store.save();
            grant(plugin, player, reward);
        }
    }

    /** Dispatches {@code reward}'s commands as console (global region thread) and sends its message. */
    private static void grant(SEssentialsPlugin plugin, Player player, RewardDefinition reward) {
        String name = player.getName();
        for (String template : reward.commands()) {
            String command = template.replace(PLAYER_PLACEHOLDER, name);
            Bukkit.getGlobalRegionScheduler().execute(plugin, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
        }
        String message = reward.message();
        if (message != null && !message.isBlank()) {
            Msg.raw(player, message);
        }
    }
}
