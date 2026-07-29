package dev.iyanz.sessentials.module.grave;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * In-memory view of the {@code grave} config section: whether death graves are
 * enabled at all, and how long (in seconds) a grave survives before its remaining
 * items are dropped back into the world.
 *
 * <p>Expected {@code config.yml} shape:</p>
 * <pre>{@code
 * grave:
 *   enabled: false        # off by default
 *   expire-seconds: 300   # seconds before an unclaimed grave drops its items
 * }</pre>
 *
 * <p>Read once at {@link GraveModule#enable}. Both fields are {@code volatile} so the
 * async expiry sweep observes them lock-free.</p>
 */
final class GraveSettings {

    /** Root config section for this module. */
    private static final String CONFIG_ROOT = "grave";

    /** Milliseconds in one second, used to convert the configured expiry window. */
    private static final long MILLIS_PER_SECOND = 1000L;

    private static final boolean DEFAULT_ENABLED = false;
    private static final long DEFAULT_EXPIRE_SECONDS = 300L;

    private volatile boolean enabled;
    private volatile long expireSeconds;

    private GraveSettings() {
    }

    /** Builds a settings instance by reading {@code grave} from {@code config}. */
    static GraveSettings load(FileConfiguration config) {
        GraveSettings settings = new GraveSettings();
        settings.enabled = config.getBoolean(CONFIG_ROOT + ".enabled", DEFAULT_ENABLED);
        settings.expireSeconds = Math.max(1L, config.getLong(CONFIG_ROOT + ".expire-seconds", DEFAULT_EXPIRE_SECONDS));
        return settings;
    }

    /** @return {@code true} if a grave should be created when an eligible player dies. */
    boolean enabled() {
        return enabled;
    }

    /** @return the grave lifetime, in milliseconds, before it expires and drops its items. */
    long expireMillis() {
        return expireSeconds * MILLIS_PER_SECOND;
    }
}
