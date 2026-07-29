package dev.iyanz.sessentials.module.combat;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which online players are currently frozen in place by staff (via
 * {@code /freeze}). Backed by a concurrent set so the command (which may run on any
 * region thread) and the move listener (which runs on each frozen player's own region
 * thread) can safely read/write it at the same time.
 */
final class FrozenPlayers {

    private final Set<UUID> frozen = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * @param playerId the player's unique id
     * @return {@code true} if that player is currently frozen
     */
    boolean isFrozen(UUID playerId) {
        return frozen.contains(playerId);
    }

    /**
     * Toggles the frozen state for the given player.
     *
     * @param playerId the player's unique id
     * @return the new state: {@code true} if now frozen, {@code false} if just unfrozen
     */
    boolean toggle(UUID playerId) {
        if (frozen.add(playerId)) {
            return true;
        }
        frozen.remove(playerId);
        return false;
    }

    /**
     * Removes the given player from the frozen set, if present. Called on quit so a
     * disconnected player's id does not linger (the set is otherwise only cleared by an
     * explicit unfreeze toggle).
     *
     * @param playerId the player's unique id
     */
    void remove(UUID playerId) {
        frozen.remove(playerId);
    }
}
