package dev.iyanz.sessentials.module.buildtools;

import java.util.UUID;

import dev.iyanz.sessentials.store.YamlStore;

/**
 * Persistent per-player {@code /silence} flags, backed by the module's
 * {@link YamlStore} under {@code silence.<uuid>}. A present key means the player's
 * own join/leave broadcasts are suppressed; absence means they are shown.
 */
final class SilenceFlags {

    /** Store path prefix for the per-player flags. */
    private static final String PATH_PREFIX = "silence.";

    private final YamlStore store;

    /**
     * @param store the module's data store
     */
    SilenceFlags(YamlStore store) {
        this.store = store;
    }

    /**
     * Flips the flag for a player and persists the change.
     *
     * @param uuid the player's id
     * @return {@code true} if the player is now silenced, {@code false} if now shown
     */
    boolean toggle(UUID uuid) {
        String path = PATH_PREFIX + uuid;
        if (store.contains(path)) {
            store.remove(path);
            store.save();
            return false;
        }
        store.set(path, true);
        store.save();
        return true;
    }

    /**
     * @param uuid the player's id
     * @return whether this player's own join/leave broadcasts are suppressed
     */
    boolean isSilenced(UUID uuid) {
        return store.contains(PATH_PREFIX + uuid);
    }
}
