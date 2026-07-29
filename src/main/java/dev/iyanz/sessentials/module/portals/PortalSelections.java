package dev.iyanz.sessentials.module.portals;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;

/**
 * Holds each player's in-progress portal-wand selection: a two-element
 * {@code [pos1, pos2]} array, either slot possibly still {@code null} until the player
 * has clicked both corners. Selections are purely in-memory and per-player (not
 * persisted), cleared implicitly whenever the player picks a new pair of points.
 */
final class PortalSelections {

    private final ConcurrentHashMap<UUID, Location[]> selections = new ConcurrentHashMap<>();

    /** Sets {@code uuid}'s first selected position, leaving the second slot untouched. */
    void setPos1(UUID uuid, Location location) {
        selections.computeIfAbsent(uuid, id -> new Location[2])[0] = location;
    }

    /** Sets {@code uuid}'s second selected position, leaving the first slot untouched. */
    void setPos2(UUID uuid, Location location) {
        selections.computeIfAbsent(uuid, id -> new Location[2])[1] = location;
    }

    /**
     * @param uuid the player's UUID
     * @return the two-element {@code [pos1, pos2]} selection array (either element may
     *         be {@code null}), or {@code null} if {@code uuid} has selected nothing yet
     */
    Location[] get(UUID uuid) {
        return selections.get(uuid);
    }

    /** Discards {@code uuid}'s in-progress selection, if any (e.g. when the player disconnects). */
    void remove(UUID uuid) {
        selections.remove(uuid);
    }
}
