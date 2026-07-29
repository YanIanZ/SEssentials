package dev.iyanz.sessentials.module.cmdcontrol;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * In-memory, reloadable view of the {@code command-control} config section: whether the
 * feature is globally enabled, and the per-command-label {@link CommandRule}s.
 *
 * <p>Expected {@code config.yml} shape:</p>
 * <pre>{@code
 * command-control:
 *   enabled: true
 *   commands:
 *     kit:                 # the command label: the first word after '/', lowercase
 *       cooldown: 60        # seconds between uses; 0 or omitted = no cooldown
 *       cost: 100.0         # money charged via Vault per allowed use; 0 or omitted = free
 *       bypass-permission: "sessentials.cmdcontrol.bypass.kit"   # optional override
 * }</pre>
 *
 * <p>A command label with no entry under {@code commands} is never touched by this
 * module. {@code bypass-permission} defaults to {@code sessentials.cmdcontrol.bypass.<label>}
 * when omitted; the global permission {@code sessentials.cmdcontrol.bypass} always bypasses
 * every configured command.</p>
 *
 * <p>Built once on module {@link CmdControlModule#enable}, and rebuilt in place by
 * {@link #reload(FileConfiguration)} on {@code /cmdcontrol reload}. Reads of {@link #enabled()}
 * and {@link #rule(String)} are lock-free and safe to call from any region thread while a
 * reload is in flight elsewhere: the backing fields are {@code volatile} and each reload
 * builds a fresh immutable map before publishing it.</p>
 */
final class CmdControlSettings {

    private static final String CONFIG_ROOT = "command-control";
    private static final String GLOBAL_BYPASS = "sessentials.cmdcontrol.bypass";

    private volatile boolean enabled;
    private volatile Map<String, CommandRule> rules;

    private CmdControlSettings(boolean enabled, Map<String, CommandRule> rules) {
        this.enabled = enabled;
        this.rules = rules;
    }

    /** Builds a settings instance by reading {@code command-control} from {@code config}. */
    static CmdControlSettings load(FileConfiguration config) {
        CmdControlSettings settings = new CmdControlSettings(true, Map.of());
        settings.reload(config);
        return settings;
    }

    /**
     * Re-reads the {@code command-control} section from {@code config} and atomically
     * replaces the in-memory enabled flag and rule set.
     *
     * @param config the plugin's current configuration (call {@code plugin.reloadConfig()}
     *               beforehand to pick up on-disk edits)
     */
    void reload(FileConfiguration config) {
        boolean newEnabled = config.getBoolean(CONFIG_ROOT + ".enabled", true);
        Map<String, CommandRule> newRules = new LinkedHashMap<>();

        ConfigurationSection commandsSection = config.getConfigurationSection(CONFIG_ROOT + ".commands");
        if (commandsSection != null) {
            for (String rawLabel : commandsSection.getKeys(false)) {
                ConfigurationSection entry = commandsSection.getConfigurationSection(rawLabel);
                if (entry == null) {
                    continue;
                }
                String label = rawLabel.toLowerCase(Locale.ROOT);
                long cooldownSeconds = Math.max(0L, entry.getLong("cooldown", 0L));
                double cost = Math.max(0.0, entry.getDouble("cost", 0.0));
                String bypassPermission = entry.getString("bypass-permission", GLOBAL_BYPASS + "." + label);
                newRules.put(label, new CommandRule(cooldownSeconds, cost, bypassPermission));
            }
        }

        this.enabled = newEnabled;
        this.rules = Map.copyOf(newRules);
    }

    /** @return whether the command-control feature is globally enabled. */
    boolean enabled() {
        return enabled;
    }

    /**
     * @param label lowercase command label (no leading slash)
     * @return the configured rule for {@code label}, or {@code null} if it has none
     */
    CommandRule rule(String label) {
        return rules.get(label);
    }
}
