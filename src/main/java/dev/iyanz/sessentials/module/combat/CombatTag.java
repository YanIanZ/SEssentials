package dev.iyanz.sessentials.module.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players are currently "combat tagged", keyed by an expiry timestamp
 * (epoch millis). Backed by a {@link ConcurrentHashMap} since the damage listener, the
 * command-preprocess listener, the quit listener and the periodic expiry sweep may all
 * touch it from different region/async threads.
 */
final class CombatTag {

    private final Map<UUID, Long> taggedUntil = new ConcurrentHashMap<>();

    /**
     * Checks whether a player is currently tagged, lazily clearing the entry if its
     * timer has already elapsed.
     *
     * @param playerId the player's unique id
     * @return {@code true} if still within their combat window
     */
    boolean isTagged(UUID playerId) {
        Long until = taggedUntil.get(playerId);
        if (until == null) {
            return false;
        }
        if (until <= System.currentTimeMillis()) {
            taggedUntil.remove(playerId, until);
            return false;
        }
        return true;
    }

    /**
     * (Re-)tags a player for {@code seconds} from now, refreshing an already-active tag.
     *
     * @param playerId the player's unique id
     * @param seconds  the tag duration in seconds
     * @return {@code true} if this call is the untagged-to-tagged transition (i.e. the
     *         player was not already actively tagged), {@code false} if it just refreshed
     *         an existing tag
     */
    boolean tag(UUID playerId, long seconds) {
        long now = System.currentTimeMillis();
        Long previous = taggedUntil.put(playerId, now + seconds * 1000L);
        return previous == null || previous <= now;
    }

    /**
     * Clears a player's tag immediately, regardless of its remaining time.
     *
     * @param playerId the player's unique id
     */
    void untag(UUID playerId) {
        taggedUntil.remove(playerId);
    }

    /**
     * Removes and returns every entry whose timer has elapsed. Intended to be polled
     * periodically so a "left combat" message can be sent the moment a tag expires,
     * rather than only when the player is next checked reactively.
     *
     * @return the unique ids of players whose tag just expired
     */
    List<UUID> sweepExpired() {
        long now = System.currentTimeMillis();
        List<UUID> expired = new ArrayList<>();
        taggedUntil.entrySet().removeIf(entry -> {
            if (entry.getValue() <= now) {
                expired.add(entry.getKey());
                return true;
            }
            return false;
        });
        return expired;
    }
}
