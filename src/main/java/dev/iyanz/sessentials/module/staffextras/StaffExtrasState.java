package dev.iyanz.sessentials.module.staffextras;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared, thread-safe state for the staff-extras module: the set of shadow-muted
 * players, the set of players mobs must ignore, and each staff member's position in
 * the {@code /patrol} cycle.
 *
 * <p>All collections are concurrent because they are written from command executors
 * (region threads) and read from asynchronous event handlers (chat fires off-region
 * on Folia). Entries are dropped on player quit via
 * {@link StaffExtrasListener}, keeping the state bounded by the online player count.</p>
 */
final class StaffExtrasState {

    /** Players whose chat is silently hidden from everyone but themselves. */
    private final Set<UUID> shadowMuted = ConcurrentHashMap.newKeySet();

    /** Players that mobs are forbidden from targeting. */
    private final Set<UUID> noTarget = ConcurrentHashMap.newKeySet();

    /** Each staff member's advance counter in the patrol cycle. */
    private final Map<UUID, Integer> patrolIndex = new ConcurrentHashMap<>();

    /**
     * Flips a player's shadow-mute flag.
     *
     * @param id the player's id
     * @return {@code true} if the player is now shadow-muted, {@code false} if unmuted
     */
    boolean toggleShadowMute(UUID id) {
        if (shadowMuted.remove(id)) {
            return false;
        }
        shadowMuted.add(id);
        return true;
    }

    /**
     * @param id the player's id
     * @return whether the player's chat is currently shadow-muted
     */
    boolean isShadowMuted(UUID id) {
        return shadowMuted.contains(id);
    }

    /**
     * Flips a player's mob no-target flag.
     *
     * @param id the player's id
     * @return {@code true} if mobs now ignore the player, {@code false} if they target again
     */
    boolean toggleNoTarget(UUID id) {
        if (noTarget.remove(id)) {
            return false;
        }
        noTarget.add(id);
        return true;
    }

    /**
     * @param id the player's id
     * @return whether mobs must ignore this player
     */
    boolean isNoTarget(UUID id) {
        return noTarget.contains(id);
    }

    /**
     * Advances and returns the staff member's patrol counter. The first call yields
     * {@code 0}, each following call one more; callers take it modulo the current
     * candidate count to pick the next player in the cycle.
     *
     * @param staffId the patrolling staff member's id
     * @return the counter value to use for this patrol hop
     */
    int nextPatrolIndex(UUID staffId) {
        return patrolIndex.compute(staffId, (k, v) -> v == null ? 0 : v + 1);
    }

    /**
     * Drops every trace of a player (called on quit so state never leaks).
     *
     * @param id the departing player's id
     */
    void forget(UUID id) {
        shadowMuted.remove(id);
        noTarget.remove(id);
        patrolIndex.remove(id);
    }
}
