package dev.iyanz.sessentials.module.afkkick;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe record of each online player's most recent activity timestamp (epoch
 * millis). Written from event listeners (which may fire on any region thread, and
 * asynchronously for chat) and read from the async sweep task, so the backing map is a
 * {@link ConcurrentHashMap}. State is purely transient and per-session: a player's
 * entry is created on join/first activity and dropped on quit or when they are kicked.
 */
final class AfkActivityTracker {

    private final ConcurrentHashMap<UUID, Long> lastActive = new ConcurrentHashMap<>();

    /**
     * Stamps {@code uuid}'s last-activity time to now. Cheap enough to call from the
     * high-frequency move listener (a single map write).
     *
     * @param uuid the player's unique id
     */
    void touch(UUID uuid) {
        lastActive.put(uuid, System.currentTimeMillis());
    }

    /**
     * @param uuid the player's unique id
     * @return the last-activity epoch millis, or {@code null} if the player is untracked
     */
    Long lastActive(UUID uuid) {
        return lastActive.get(uuid);
    }

    /**
     * Drops a player's tracking entry, e.g. on quit or after a kick.
     *
     * @param uuid the player's unique id
     */
    void remove(UUID uuid) {
        lastActive.remove(uuid);
    }

    /** Clears all tracked activity, e.g. on plugin disable. */
    void clear() {
        lastActive.clear();
    }
}
