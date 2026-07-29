package dev.iyanz.sessentials.module.afkkick;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * In-memory, reloadable view of the {@code afk-kick} config section: whether the
 * feature is enabled, how long a player may stay idle before being kicked, the kick
 * message, and the permission that exempts a player from being kicked.
 *
 * <p>Expected {@code config.yml} shape:</p>
 * <pre>{@code
 * afk-kick:
 *   enabled: false                                  # off by default
 *   seconds: 600                                    # idle seconds before a kick
 *   message: "You were kicked for being AFK."       # shown on the kick screen
 *   bypass-permission: "sessentials.afkkick.bypass" # holders are never kicked
 * }</pre>
 *
 * <p>Built once on module {@link AfkKickModule#enable}, and rebuilt in place by
 * {@link #reload(FileConfiguration)} on {@code /afkkick reload}. Every field is
 * {@code volatile} so reads from the async sweep task or the (potentially async) chat
 * listener stay lock-free and observe a completed reload atomically per field.</p>
 */
final class AfkKickSettings {

    /** Root config section for this module. */
    private static final String CONFIG_ROOT = "afk-kick";

    /** Milliseconds in one second, used to convert the configured idle window. */
    private static final long MILLIS_PER_SECOND = 1000L;

    private static final boolean DEFAULT_ENABLED = false;
    private static final long DEFAULT_SECONDS = 600L;
    private static final String DEFAULT_MESSAGE = "You were kicked for being AFK.";
    private static final String DEFAULT_BYPASS = "sessentials.afkkick.bypass";

    private volatile boolean enabled;
    private volatile long seconds;
    private volatile String message;
    private volatile String bypassPermission;

    private AfkKickSettings() {
    }

    /** Builds a settings instance by reading {@code afk-kick} from {@code config}. */
    static AfkKickSettings load(FileConfiguration config) {
        AfkKickSettings settings = new AfkKickSettings();
        settings.reload(config);
        return settings;
    }

    /**
     * Re-reads the {@code afk-kick} section from {@code config} and replaces the
     * in-memory values.
     *
     * @param config the plugin's current configuration (call {@code plugin.reloadConfig()}
     *               beforehand to pick up on-disk edits)
     */
    void reload(FileConfiguration config) {
        this.enabled = config.getBoolean(CONFIG_ROOT + ".enabled", DEFAULT_ENABLED);
        this.seconds = Math.max(1L, config.getLong(CONFIG_ROOT + ".seconds", DEFAULT_SECONDS));
        this.message = config.getString(CONFIG_ROOT + ".message", DEFAULT_MESSAGE);
        this.bypassPermission = config.getString(CONFIG_ROOT + ".bypass-permission", DEFAULT_BYPASS);
    }

    /** @return {@code true} if idle players should be tracked and kicked. */
    boolean enabled() {
        return enabled;
    }

    /** @return the idle window, in milliseconds, before a kick is issued. */
    long idleMillis() {
        return seconds * MILLIS_PER_SECOND;
    }

    /** @return the plain-text message shown to a kicked player. */
    String message() {
        return message;
    }

    /** @return the permission node that exempts a player from AFK kicks. */
    String bypassPermission() {
        return bypassPermission;
    }
}
