package dev.iyanz.sessentials.module.teleport;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;

/**
 * Thread-safe, in-memory store of each player's "previous location", used by
 * {@code /back}. Updated by {@link TeleportHistoryListener} whenever a player
 * teleports (the location they teleported <em>from</em>) or dies (the location
 * they died at).
 */
final class TeleportHistory {

    private final Map<UUID, Location> previous = new ConcurrentHashMap<>();

    /**
     * Records {@code location} as the player's new {@code /back} target.
     *
     * @param playerId the player's UUID
     * @param location the location to remember (defensively cloned)
     */
    void remember(UUID playerId, Location location) {
        previous.put(playerId, location.clone());
    }

    /**
     * @param playerId the player's UUID
     * @return the stored location, or {@code null} if none is recorded
     */
    Location get(UUID playerId) {
        return previous.get(playerId);
    }
}
