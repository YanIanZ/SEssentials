package dev.iyanz.sessentials.module.playerstate;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players currently have god mode (damage invulnerability) enabled.
 *
 * <p>Backed by a concurrent set: on Folia, the {@code /god} command and the damage
 * listener that consults it may run on different region threads at the same time.</p>
 */
final class GodService {

    private final Set<UUID> godPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * @param playerId the player's unique id
     * @return {@code true} if that player currently has god mode enabled
     */
    boolean isGod(UUID playerId) {
        return godPlayers.contains(playerId);
    }

    /**
     * Toggles god mode for the given player.
     *
     * @param playerId the player's unique id
     * @return the new state: {@code true} if god mode is now enabled, {@code false} if
     *         it was just disabled
     */
    boolean toggle(UUID playerId) {
        if (godPlayers.add(playerId)) {
            return true;
        }
        godPlayers.remove(playerId);
        return false;
    }

    /**
     * Enables god mode for the given player, idempotently. Used to restore god mode for a
     * player whose flag was persisted across a relog (see {@link GodJoinListener}).
     *
     * @param playerId the player's unique id
     */
    void add(UUID playerId) {
        godPlayers.add(playerId);
    }

    /**
     * Disables god mode for the given player, keeping the registry bounded when a player
     * disconnects. A no-op if the player was not in god mode.
     *
     * @param playerId the player's unique id
     */
    void remove(UUID playerId) {
        godPlayers.remove(playerId);
    }
}
