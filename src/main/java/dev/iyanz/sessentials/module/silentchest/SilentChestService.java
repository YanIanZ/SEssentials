package dev.iyanz.sessentials.module.silentchest;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players currently have silent-chest (peek) mode enabled.
 *
 * <p>Backed by a concurrent set: on Folia, the {@code /silentchest} command and the
 * interact listener that consults it may run on different region threads at the
 * same time. Membership is purely in-memory and does not survive a restart —
 * silent mode is a per-session toggle, not a persisted preference.</p>
 */
final class SilentChestService {

    private final Set<UUID> silentPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * @param playerId the player's unique id
     * @return {@code true} if that player currently has silent-chest mode enabled
     */
    boolean isSilent(UUID playerId) {
        return silentPlayers.contains(playerId);
    }

    /**
     * Toggles silent-chest mode for the given player.
     *
     * @param playerId the player's unique id
     * @return the new state: {@code true} if silent mode is now enabled, {@code false}
     *         if it was just disabled
     */
    boolean toggle(UUID playerId) {
        if (silentPlayers.add(playerId)) {
            return true;
        }
        silentPlayers.remove(playerId);
        return false;
    }

    /**
     * Clears silent-chest mode for the given player, if set. Called on quit so a
     * disconnected player's id does not leak from the in-memory set.
     *
     * @param playerId the player's unique id
     */
    void remove(UUID playerId) {
        silentPlayers.remove(playerId);
    }
}
