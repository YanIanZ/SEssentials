package dev.iyanz.sessentials.module.trade;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.entity.Player;

/**
 * Central registry for the trade module: pending {@code /trade} requests (in-memory,
 * {@value #REQUEST_EXPIRY_MILLIS} ms expiry) and the currently open {@link TradeSession}s
 * keyed by each participant's UUID. A player may be in at most one request-target slot and
 * one live session at a time.
 */
final class TradeManager {

    /** How long a pending trade request stays valid. */
    static final long REQUEST_EXPIRY_MILLIS = 60_000L;

    /** A pending trade request directed at a target. */
    private record Request(UUID requester, String requesterName, long createdAtMillis) {
        boolean expired() {
            return System.currentTimeMillis() - createdAtMillis > REQUEST_EXPIRY_MILLIS;
        }
    }

    private final SEssentialsPlugin plugin;
    /** Keyed by the target's UUID (the player who runs {@code /tradeaccept}). */
    private final ConcurrentHashMap<UUID, Request> requests = new ConcurrentHashMap<>();
    /** Keyed by <em>both</em> participants' UUIDs → the shared session. */
    private final ConcurrentHashMap<UUID, TradeSession> sessions = new ConcurrentHashMap<>();

    TradeManager(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /** @return the live session {@code playerId} is part of, or {@code null}. */
    TradeSession sessionOf(UUID playerId) {
        return sessions.get(playerId);
    }

    /** @return whether {@code playerId} is currently in a trade GUI. */
    boolean inSession(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    /**
     * Records a trade request from {@code requester} directed at {@code target}, replacing
     * any prior request aimed at that target.
     *
     * @param requester the player asking to trade
     * @param target    the player who must accept
     */
    void request(Player requester, Player target) {
        requests.put(target.getUniqueId(),
                new Request(requester.getUniqueId(), requester.getName(), System.currentTimeMillis()));
    }

    /**
     * Accepts the pending request aimed at {@code target} and opens the shared trade GUI.
     * Validates that the request exists, has not expired, the requester is still online,
     * and neither player is already mid-trade.
     *
     * @param target the accepting player (ran {@code /tradeaccept})
     */
    void accept(Player target) {
        Request req = requests.remove(target.getUniqueId());
        if (req == null || req.expired()) {
            Msg.err(target, "You have no pending trade request.");
            return;
        }
        Player requester = plugin.getServer().getPlayer(req.requester());
        if (requester == null) {
            Msg.err(target, req.requesterName() + " is no longer online.");
            return;
        }
        if (requester.equals(target)) {
            Msg.err(target, "You cannot trade with yourself.");
            return;
        }
        if (inSession(target.getUniqueId())) {
            Msg.err(target, "You are already in a trade.");
            return;
        }
        if (inSession(requester.getUniqueId())) {
            Msg.err(target, requester.getName() + " is already in a trade.");
            return;
        }

        // The accepting player is LEFT; the original requester is RIGHT.
        TradeMenu leftMenu = new TradeMenu(target.getUniqueId(), target.getName(), requester.getName());
        TradeMenu rightMenu = new TradeMenu(requester.getUniqueId(), requester.getName(), target.getName());
        TradeSession session = new TradeSession(plugin, this,
                target.getUniqueId(), target.getName(), leftMenu,
                requester.getUniqueId(), requester.getName(), rightMenu);
        leftMenu.bind(session, TradeMenu.Side.LEFT);
        rightMenu.bind(session, TradeMenu.Side.RIGHT);

        sessions.put(target.getUniqueId(), session);
        sessions.put(requester.getUniqueId(), session);
        session.open();
    }

    /**
     * Removes a finished session from the registry (idempotent).
     *
     * @param session the session to forget
     */
    void remove(TradeSession session) {
        sessions.remove(session.leftId(), session);
        sessions.remove(session.rightId(), session);
    }

    /**
     * Drops every pending trade request that involves the given player, whether as the
     * <em>target</em> (their pending map entry) or as the <em>requester</em> (any request
     * they sent to someone else). Called on disconnect so the request map does not leak
     * entries for ignored requests. Does not touch live sessions.
     *
     * @param playerId the UUID of the player who left
     */
    void removeRequests(UUID playerId) {
        requests.remove(playerId);
        requests.values().removeIf(req -> req.requester().equals(playerId));
    }
}
