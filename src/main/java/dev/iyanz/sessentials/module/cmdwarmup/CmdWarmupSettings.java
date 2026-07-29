package dev.iyanz.sessentials.module.cmdwarmup;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * In-memory, reloadable view of the {@code command-warmup} config section: whether the
 * feature is globally enabled, and the per-command-label warmup duration in seconds.
 *
 * <p>Expected {@code config.yml} shape:</p>
 * <pre>{@code
 * command-warmup:
 *   enabled: true
 *   commands:
 *     warp: 3        # seconds of warmup before /warp runs
 *     home: 3
 * }</pre>
 *
 * <p>A command label with no entry under {@code commands} (or a non-positive value) is
 * never touched by this module.</p>
 *
 * <p>Built once on module {@link CmdWarmupModule#enable}, and rebuilt in place by
 * {@link #reload(FileConfiguration)} on {@code /cmdwarmup reload}. Reads of {@link #enabled()}
 * and {@link #seconds(String)} are lock-free and safe to call from any region thread while a
 * reload is in flight elsewhere: the backing fields are {@code volatile} and each reload
 * builds a fresh immutable map before publishing it.</p>
 */
final class CmdWarmupSettings {

    private static final String CONFIG_ROOT = "command-warmup";

    private volatile boolean enabled;
    private volatile Map<String, Long> warmups;

    private CmdWarmupSettings(boolean enabled, Map<String, Long> warmups) {
        this.enabled = enabled;
        this.warmups = warmups;
    }

    /** Builds a settings instance by reading {@code command-warmup} from {@code config}. */
    static CmdWarmupSettings load(FileConfiguration config) {
        CmdWarmupSettings settings = new CmdWarmupSettings(true, Map.of());
        settings.reload(config);
        return settings;
    }

    /**
     * Re-reads the {@code command-warmup} section from {@code config} and atomically
     * replaces the in-memory enabled flag and warmup durations.
     *
     * @param config the plugin's current configuration (call {@code plugin.reloadConfig()}
     *               beforehand to pick up on-disk edits)
     */
    void reload(FileConfiguration config) {
        boolean newEnabled = config.getBoolean(CONFIG_ROOT + ".enabled", true);
        Map<String, Long> newWarmups = new LinkedHashMap<>();

        ConfigurationSection commandsSection = config.getConfigurationSection(CONFIG_ROOT + ".commands");
        if (commandsSection != null) {
            for (String rawLabel : commandsSection.getKeys(false)) {
                long seconds = commandsSection.getLong(rawLabel, 0L);
                if (seconds > 0L) {
                    newWarmups.put(rawLabel.toLowerCase(Locale.ROOT), seconds);
                }
            }
        }

        this.enabled = newEnabled;
        this.warmups = Map.copyOf(newWarmups);
    }

    /** @return whether the command-warmup feature is globally enabled. */
    boolean enabled() {
        return enabled;
    }

    /**
     * @param label lowercase command label (no leading slash)
     * @return the configured warmup duration in seconds for {@code label}, or {@code null} if none
     */
    Long seconds(String label) {
        return warmups.get(label);
    }
}
