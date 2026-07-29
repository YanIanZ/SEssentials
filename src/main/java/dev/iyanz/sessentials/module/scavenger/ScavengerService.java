package dev.iyanz.sessentials.module.scavenger;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.iyanz.sessentials.store.YamlStore;

/**
 * Tracks which players currently have "scavenger" (keep-inventory-on-death) mode
 * toggled on, persisted in the {@code scavenger} data store under the
 * {@code "enabled"} key as a list of player UUID strings.
 *
 * <p>Backed by an in-memory concurrent set loaded once at startup: on Folia the
 * {@code /scavenger} command and the death listener that consults it may run on
 * different region threads at the same time.</p>
 */
final class ScavengerService {

    private static final String ENABLED_PATH = "enabled";

    private final YamlStore store;
    private final Set<UUID> enabled = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * @param store the {@code scavenger} data store; its {@code "enabled"} list is
     *              loaded into memory immediately
     */
    ScavengerService(YamlStore store) {
        this.store = store;
        for (String raw : store.config().getStringList(ENABLED_PATH)) {
            try {
                enabled.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries rather than fail plugin startup
            }
        }
    }

    /**
     * @param playerId the player's unique id
     * @return {@code true} if that player currently has scavenger mode enabled
     */
    boolean isEnabled(UUID playerId) {
        return enabled.contains(playerId);
    }

    /**
     * Toggles scavenger mode for the given player and persists the change.
     *
     * @param playerId the player's unique id
     * @return the new state: {@code true} if scavenger mode is now enabled,
     *         {@code false} if it was just disabled
     */
    boolean toggle(UUID playerId) {
        boolean nowEnabled;
        if (enabled.add(playerId)) {
            nowEnabled = true;
        } else {
            enabled.remove(playerId);
            nowEnabled = false;
        }
        persist();
        return nowEnabled;
    }

    /** Writes the current in-memory set to the backing store and saves it. */
    private void persist() {
        List<String> ids = enabled.stream().map(UUID::toString).toList();
        store.set(ENABLED_PATH, ids);
        store.save();
    }
}
