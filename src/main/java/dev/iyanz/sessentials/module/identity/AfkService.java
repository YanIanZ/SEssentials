package dev.iyanz.sessentials.module.identity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which online players are currently marked AFK. Purely in-memory and
 * transient: state is per-session and is cleared when a player disconnects (see
 * {@link IdentityListener#onQuit}), so nobody rejoins already marked AFK.
 */
public final class AfkService {

    private final Set<UUID> afk = ConcurrentHashMap.newKeySet();

    /**
     * Flips a player's AFK state.
     *
     * @param uuid the player's unique id
     * @return {@code true} if the player is now AFK, {@code false} if they just left AFK
     */
    public boolean toggle(UUID uuid) {
        if (afk.remove(uuid)) {
            return false;
        }
        afk.add(uuid);
        return true;
    }

    /**
     * @param uuid the player's unique id
     * @return {@code true} if that player is currently marked AFK
     */
    public boolean isAfk(UUID uuid) {
        return afk.contains(uuid);
    }

    /**
     * Clears a player's AFK state, e.g. on disconnect.
     *
     * @param uuid the player's unique id
     */
    public void clear(UUID uuid) {
        afk.remove(uuid);
    }
}
