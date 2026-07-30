package dev.iyanz.sessentials.module.playerstate;

import java.util.UUID;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.store.YamlStore;

/**
 * Persistence helpers for CMI-style cross-relog god mode. Keeps the "should this player's
 * god flag survive a relog?" state in the shared {@code playerstate} data store at
 * {@code god.<uuid>: true}, so {@link GodQuitListener}, {@link GodJoinListener} and the
 * {@code /god} command all agree on the store name, path and config key.
 *
 * <p>The in-memory {@link GodService} remains the runtime source of truth; this store is
 * only <em>written</em> on quit (and cleared by {@code /god off}) and <em>read</em> on
 * join. All access goes through the monitor-guarded {@link YamlStore} accessors, so it is
 * Folia-safe from any region thread.</p>
 */
final class GodPersistence {

    /** Config key gating cross-relog god persistence (default {@code true}). */
    static final String CONFIG_PERSIST = "god.persist";

    /** Shared data store holding the persisted flags ({@code playerstate.yml}). */
    private static final String STORE = "playerstate";

    /** Path prefix for a player's persisted god flag. */
    private static final String PREFIX = "god.";

    private GodPersistence() {
    }

    /** @return whether cross-relog god persistence is enabled in config. */
    static boolean enabled(SEssentialsPlugin plugin) {
        return plugin.getConfig().getBoolean(CONFIG_PERSIST, true);
    }

    /** @return whether {@code playerId} has a persisted god flag in the store. */
    static boolean isStored(SEssentialsPlugin plugin, UUID playerId) {
        return plugin.stores().get(STORE).contains(PREFIX + playerId);
    }

    /** Persists {@code playerId}'s god flag ({@code god.<uuid>: true}) and saves. */
    static void store(SEssentialsPlugin plugin, UUID playerId) {
        YamlStore store = plugin.stores().get(STORE);
        store.set(PREFIX + playerId, true);
        store.save();
    }

    /** Clears {@code playerId}'s persisted god flag and saves. */
    static void clear(SEssentialsPlugin plugin, UUID playerId) {
        YamlStore store = plugin.stores().get(STORE);
        store.remove(PREFIX + playerId);
        store.save();
    }
}
