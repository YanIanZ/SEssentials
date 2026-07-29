package dev.iyanz.sessentials.module.playtimerewards;

import java.util.ArrayList;
import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * In-memory, reloadable view of the {@code playtime-rewards} config section: whether
 * the feature is globally enabled, how often the background check runs, and the
 * configured {@link RewardDefinition}s.
 *
 * <p>Expected {@code config.yml} shape:</p>
 * <pre>{@code
 * playtime-rewards:
 *   enabled: true
 *   check-interval-seconds: 60
 *   rewards:
 *     <id>:
 *       minutes: 60
 *       commands: ["give %player% diamond 1", "eco give %player% 500"]
 *       message: "You reached 1 hour of playtime!"
 * }</pre>
 *
 * <p>Built once on module {@link PlaytimeRewardsModule#enable}, and rebuilt in place by
 * {@link #reload(FileConfiguration)} on {@code /playtimerewards reload}. Reads are
 * lock-free and safe from any region thread while a reload is in flight elsewhere: the
 * backing fields are {@code volatile} and each reload builds fresh immutable data
 * before publishing it.</p>
 */
final class PlaytimeRewardsSettings {

    private static final String CONFIG_ROOT = "playtime-rewards";
    private static final long DEFAULT_INTERVAL_SECONDS = 60L;

    /** Id, milestone and message of the demonstrative reward seeded when none is configured. */
    private static final String DEFAULT_REWARD_ID = "onehour";
    private static final long DEFAULT_REWARD_MINUTES = 60L;
    private static final String DEFAULT_REWARD_MESSAGE = "You reached 1 hour of playtime!";

    private volatile boolean enabled;
    private volatile long checkIntervalSeconds;
    private volatile List<RewardDefinition> rewards;

    private PlaytimeRewardsSettings() {
    }

    /** Builds a settings instance by reading {@code playtime-rewards} from {@code config}. */
    static PlaytimeRewardsSettings load(FileConfiguration config) {
        PlaytimeRewardsSettings settings = new PlaytimeRewardsSettings();
        settings.reload(config);
        return settings;
    }

    /**
     * Re-reads the {@code playtime-rewards} section from {@code config} and atomically
     * replaces the in-memory enabled flag, interval and reward list.
     *
     * @param config the plugin's current configuration (call {@code plugin.reloadConfig()}
     *               beforehand to pick up on-disk edits)
     */
    void reload(FileConfiguration config) {
        boolean newEnabled = config.getBoolean(CONFIG_ROOT + ".enabled", true);
        long newInterval = Math.max(1L, config.getLong(CONFIG_ROOT + ".check-interval-seconds", DEFAULT_INTERVAL_SECONDS));

        List<RewardDefinition> loaded = new ArrayList<>();
        ConfigurationSection rewardsSection = config.getConfigurationSection(CONFIG_ROOT + ".rewards");
        if (rewardsSection != null) {
            for (String id : rewardsSection.getKeys(false)) {
                ConfigurationSection entry = rewardsSection.getConfigurationSection(id);
                if (entry == null) {
                    continue;
                }
                long minutes = Math.max(0L, entry.getLong("minutes", 0L));
                List<String> commands = List.copyOf(entry.getStringList("commands"));
                String message = entry.getString("message", "");
                loaded.add(new RewardDefinition(id, minutes, commands, message));
            }
        }
        loaded.sort((a, b) -> Long.compare(a.minutes(), b.minutes()));

        this.enabled = newEnabled;
        this.checkIntervalSeconds = newInterval;
        this.rewards = List.copyOf(loaded);
    }

    /**
     * Writes one demonstrative, message-only reward (a 60-minute milestone) into the
     * plugin config if {@code playtime-rewards.rewards} is unset or empty, then saves
     * the file so operators immediately see and can edit a working example. No-op if
     * any reward is already configured.
     *
     * @param plugin the owning plugin, whose config is mutated and persisted
     */
    static void seedDefaultIfEmpty(SEssentialsPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection rewardsSection = config.getConfigurationSection(CONFIG_ROOT + ".rewards");
        if (rewardsSection != null && !rewardsSection.getKeys(false).isEmpty()) {
            return;
        }
        String path = CONFIG_ROOT + ".rewards." + DEFAULT_REWARD_ID;
        config.set(path + ".minutes", DEFAULT_REWARD_MINUTES);
        config.set(path + ".commands", List.of());
        config.set(path + ".message", DEFAULT_REWARD_MESSAGE);
        plugin.saveConfig();
    }

    /** @return whether the playtime-rewards feature is globally enabled. */
    boolean enabled() {
        return enabled;
    }

    /** @return seconds between background checks of every online player. */
    long checkIntervalSeconds() {
        return checkIntervalSeconds;
    }

    /** @return every configured reward, ascending by required minutes. */
    List<RewardDefinition> rewards() {
        return rewards;
    }
}
