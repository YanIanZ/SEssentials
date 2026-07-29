package dev.iyanz.sessentials.module.timedcommands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * In-memory, reloadable view of the {@code timed-commands} config section: whether the
 * feature is globally enabled and the configured {@link TimedTaskDefinition}s.
 *
 * <p>Expected {@code config.yml} shape:</p>
 * <pre>{@code
 * timed-commands:
 *   enabled: true
 *   tasks:
 *     <id>:
 *       interval-seconds: 3600
 *       commands: ["say Server auto-save", "sess reload"]
 * }</pre>
 *
 * <p>Built once on module enable, and rebuilt in place by {@link #reload(FileConfiguration)}
 * on {@code /timedcommands reload}. Reads are lock-free and safe from any region thread
 * while a reload is in flight elsewhere: the backing fields are {@code volatile} and each
 * reload builds fresh immutable data before publishing it.</p>
 */
final class TimedCommandsSettings {

    private static final String CONFIG_ROOT = "timed-commands";

    /** Fallback used only when a configured task omits {@code interval-seconds} or sets it to 0. */
    private static final long DEFAULT_INTERVAL_SECONDS = 60L;

    private volatile boolean enabled;
    private volatile List<TimedTaskDefinition> tasks;

    private TimedCommandsSettings() {
    }

    /** Builds a settings instance by reading {@code timed-commands} from {@code config}. */
    static TimedCommandsSettings load(FileConfiguration config) {
        TimedCommandsSettings settings = new TimedCommandsSettings();
        settings.reload(config);
        return settings;
    }

    /**
     * Re-reads the {@code timed-commands} section from {@code config} and atomically
     * replaces the in-memory enabled flag and task list.
     *
     * @param config the plugin's current configuration (call {@code plugin.reloadConfig()}
     *               beforehand to pick up on-disk edits)
     */
    void reload(FileConfiguration config) {
        boolean newEnabled = config.getBoolean(CONFIG_ROOT + ".enabled", true);

        List<TimedTaskDefinition> loaded = new ArrayList<>();
        ConfigurationSection tasksSection = config.getConfigurationSection(CONFIG_ROOT + ".tasks");
        if (tasksSection != null) {
            for (String id : tasksSection.getKeys(false)) {
                ConfigurationSection entry = tasksSection.getConfigurationSection(id);
                if (entry == null) {
                    continue;
                }
                long intervalSeconds = entry.getLong("interval-seconds", DEFAULT_INTERVAL_SECONDS);
                if (intervalSeconds <= 0L) {
                    intervalSeconds = DEFAULT_INTERVAL_SECONDS;
                }
                List<String> commands = List.copyOf(entry.getStringList("commands"));
                loaded.add(new TimedTaskDefinition(id, intervalSeconds, commands));
            }
        }

        this.enabled = newEnabled;
        this.tasks = List.copyOf(loaded);
    }

    /** @return whether the timed-commands feature is globally enabled. */
    boolean enabled() {
        return enabled;
    }

    /** @return every configured task, in config-file order; empty (never {@code null}) if none are set. */
    List<TimedTaskDefinition> tasks() {
        return tasks;
    }
}
