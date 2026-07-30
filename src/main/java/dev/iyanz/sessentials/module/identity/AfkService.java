package dev.iyanz.sessentials.module.identity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which online players are currently marked AFK, and whether that state was
 * set manually (via {@code /afk}) or automatically (by the idle sweep, see
 * {@link AutoAfk}). Purely in-memory and transient: state is per-session and is
 * cleared when a player disconnects (see {@link IdentityListener#onQuit}), so nobody
 * rejoins already marked AFK.
 *
 * <p>The backing map is a {@link ConcurrentHashMap} keyed by player UUID; a present
 * entry means the player is AFK and its value records whether the mark was automatic
 * ({@code true}) or manual ({@code false}). It is read and written from several
 * threads — the (region-thread) command executor, the async idle sweep and the
 * activity listeners — so every mutation used by the auto path is a single atomic map
 * operation ({@link Map#putIfAbsent}, {@link Map#remove(Object, Object)}) rather than a
 * check-then-act.</p>
 */
public final class AfkService {

    /** Marker for an automatically-set AFK entry (idle sweep). */
    private static final Boolean AUTO = Boolean.TRUE;
    /** Marker for a manually-set AFK entry ({@code /afk}). */
    private static final Boolean MANUAL = Boolean.FALSE;

    /** UUID -&gt; {@link #AUTO}/{@link #MANUAL}; presence means the player is AFK. */
    private final ConcurrentHashMap<UUID, Boolean> state = new ConcurrentHashMap<>();

    /**
     * Flips a player's AFK state as a <em>manual</em> toggle (the {@code /afk} path):
     * if they were AFK (whether auto or manual) they are cleared, otherwise they are
     * marked AFK and tagged as a manual mark so a later activity sweep won't auto-clear
     * them.
     *
     * @param uuid the player's unique id
     * @return {@code true} if the player is now AFK, {@code false} if they just left AFK
     */
    public boolean toggle(UUID uuid) {
        if (state.remove(uuid) != null) {
            return false;
        }
        state.put(uuid, MANUAL);
        return true;
    }

    /**
     * Automatically marks a player AFK, but only if they are not already AFK. The check
     * and insert are a single atomic {@link Map#putIfAbsent} so concurrent sweeps (or a
     * sweep racing a manual toggle) can never double-mark or clobber a manual mark.
     *
     * @param uuid the player's unique id
     * @return {@code true} if this call actually marked the player AFK (so the caller
     *         should broadcast); {@code false} if they were already AFK
     */
    public boolean markAuto(UUID uuid) {
        return state.putIfAbsent(uuid, AUTO) == null;
    }

    /**
     * Clears a player's AFK state only if it was set automatically, leaving a manual
     * {@code /afk} untouched. Used by the activity path so that returning from idle
     * auto-unmarks an auto-AFK player, while a manual AFK stays until they run
     * {@code /afk} again (matching CMI). Atomic via {@link Map#remove(Object, Object)}.
     *
     * @param uuid the player's unique id
     * @return {@code true} if an auto mark was cleared (so the caller should broadcast);
     *         {@code false} if the player was not AFK, or was AFK manually
     */
    public boolean clearIfAuto(UUID uuid) {
        return state.remove(uuid, AUTO);
    }

    /**
     * @param uuid the player's unique id
     * @return {@code true} if that player is currently marked AFK (auto or manual)
     */
    public boolean isAfk(UUID uuid) {
        return state.containsKey(uuid);
    }

    /**
     * Clears a player's AFK state unconditionally, e.g. on disconnect.
     *
     * @param uuid the player's unique id
     */
    public void clear(UUID uuid) {
        state.remove(uuid);
    }
}
