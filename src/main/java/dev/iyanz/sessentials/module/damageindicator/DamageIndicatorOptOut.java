package dev.iyanz.sessentials.module.damageindicator;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players have opted out of having damage numbers appear above
 * themselves.
 *
 * <p>Deliberately in-memory only (a concurrent {@link Set} of player UUIDs): the
 * opt-out is a lightweight, session-scoped preference, so it is not persisted and is
 * cleared when the player quits (see {@link DamageIndicatorListener}). The set is
 * concurrent because damage events on Folia may be read from any region thread while
 * the {@code /damageindicator} command mutates it from another.</p>
 */
final class DamageIndicatorOptOut {

    private final Set<UUID> hidden = ConcurrentHashMap.newKeySet();

    /**
     * @param playerId the player's unique id
     * @return {@code true} if that player has opted out of damage numbers on themselves
     */
    boolean isHidden(UUID playerId) {
        return hidden.contains(playerId);
    }

    /**
     * Toggles a player's opt-out state.
     *
     * @param playerId the player's unique id
     * @return the new state: {@code true} if now hidden (opted out), {@code false} if now shown
     */
    boolean toggle(UUID playerId) {
        if (hidden.remove(playerId)) {
            return false;
        }
        hidden.add(playerId);
        return true;
    }

    /**
     * Forgets a player's opt-out state (e.g. on quit).
     *
     * @param playerId the player's unique id
     */
    void clear(UUID playerId) {
        hidden.remove(playerId);
    }
}
