package dev.iyanz.sessentials.module.teleport;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry of pending teleport requests ({@code /tpa}, {@code /tpahere}),
 * keyed by the UUID of the player who must accept or deny the request (the request's
 * <em>target</em>). Requests expire {@value #EXPIRY_MILLIS} ms after creation.
 *
 * <p>Only one pending request per target is kept; a new request silently replaces
 * any previous one.</p>
 */
final class TeleportRequests {

    /** How long a pending request stays valid before it is treated as expired. */
    static final long EXPIRY_MILLIS = 60_000L;

    /** What {@code /tpaccept} performs once a request of this kind is accepted. */
    enum Kind {
        /** The requester teleports to the target ({@code /tpa}). */
        TO_TARGET,
        /** The target teleports to the requester ({@code /tpahere}). */
        TARGET_TO_REQUESTER
    }

    /** A single pending teleport request. */
    record Request(UUID requester, Kind kind, long createdAtMillis) {

        /** @return whether this request is older than {@link #EXPIRY_MILLIS}. */
        boolean expired() {
            return System.currentTimeMillis() - createdAtMillis > EXPIRY_MILLIS;
        }
    }

    private final ConcurrentHashMap<UUID, Request> pending = new ConcurrentHashMap<>();

    /**
     * Records a new pending request directed at {@code target}, replacing any prior one.
     *
     * @param target    the player who must accept/deny (recipient of the prompt)
     * @param requester the player who asked for the teleport
     * @param kind      what happens on acceptance
     */
    void request(UUID target, UUID requester, Kind kind) {
        pending.put(target, new Request(requester, kind, System.currentTimeMillis()));
    }

    /**
     * Removes and returns the pending request for {@code target}, if any and not expired.
     *
     * @param target the player who was asked to accept/deny
     * @return the request, or {@code null} if none is pending or it already expired
     */
    Request take(UUID target) {
        Request req = pending.remove(target);
        if (req == null || req.expired()) {
            return null;
        }
        return req;
    }

    /**
     * Drops every pending request that involves the given player, whether as the
     * <em>target</em> (their pending map entry) or as the <em>requester</em> (any request
     * they sent to someone else). Called on disconnect so the map does not leak entries for
     * ignored requests.
     *
     * @param playerId the UUID of the player who left
     */
    void removePlayer(UUID playerId) {
        pending.remove(playerId);
        pending.values().removeIf(req -> req.requester().equals(playerId));
    }
}
