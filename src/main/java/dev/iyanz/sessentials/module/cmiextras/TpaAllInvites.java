package dev.iyanz.sessentials.module.cmiextras;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry of pending {@code /tpaall} invites, keyed by the UUID of the
 * invited player. Each invite records who sent it and when; invites expire
 * {@value #EXPIRY_MILLIS} ms after creation and a newer invite silently replaces an
 * older one for the same player.
 *
 * <p>This registry is deliberately self-contained (the teleport module's request store
 * is package-private and this module may not modify other packages), so acceptance goes
 * through {@code /tpaall accept} rather than {@code /tpaccept}.</p>
 */
final class TpaAllInvites {

    /** How long an invite stays valid before it is treated as expired. */
    static final long EXPIRY_MILLIS = 60_000L;

    /** A single pending invite from {@code requester}, created at {@code createdAtMillis}. */
    record Invite(UUID requester, long createdAtMillis) {

        /** @return whether this invite is older than {@link #EXPIRY_MILLIS}. */
        boolean expired() {
            return System.currentTimeMillis() - createdAtMillis > EXPIRY_MILLIS;
        }
    }

    private final ConcurrentHashMap<UUID, Invite> pending = new ConcurrentHashMap<>();

    /**
     * Records an invite directed at {@code target}, replacing any prior one.
     *
     * @param target    the invited player (who may accept)
     * @param requester the player everyone would teleport to
     */
    void invite(UUID target, UUID requester) {
        pending.put(target, new Invite(requester, System.currentTimeMillis()));
    }

    /**
     * Removes and returns the pending invite for {@code target}, if any and not expired.
     *
     * @param target the invited player
     * @return the invite, or {@code null} if none is pending or it already expired
     */
    Invite take(UUID target) {
        Invite invite = pending.remove(target);
        if (invite == null || invite.expired()) {
            return null;
        }
        return invite;
    }

    /**
     * Drops every pending invite involving the given player — their own entry and any
     * invite they sent to others. Called on disconnect so the map never leaks entries.
     *
     * @param playerId the UUID of the player who left
     */
    void removePlayer(UUID playerId) {
        pending.remove(playerId);
        pending.values().removeIf(invite -> invite.requester().equals(playerId));
    }
}
