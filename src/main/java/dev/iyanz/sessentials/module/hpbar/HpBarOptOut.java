package dev.iyanz.sessentials.module.hpbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.store.YamlStore;

/**
 * Tracks which players have opted out of seeing the HP bar action-bar readout.
 *
 * <p>Backed by a single list of lowercased UUID strings at path {@code "disabled"} in
 * the {@code "hpbar"} data store (via {@link dev.iyanz.sessentials.store.Stores}), so
 * the opt-out set survives restarts. Reads/writes go through the store's in-memory
 * {@link YamlStore#config()}, so calls are cheap; {@link YamlStore#save()} debounces
 * the actual (async) disk write.</p>
 */
final class HpBarOptOut {

    private static final String PATH = "disabled";

    private HpBarOptOut() {
    }

    /**
     * @param plugin   the owning plugin, used to reach the {@code "hpbar"} data store
     * @param playerId the attacker's unique id
     * @return {@code true} if that player has opted out of the HP bar readout
     */
    static boolean isDisabled(SEssentialsPlugin plugin, UUID playerId) {
        String id = playerId.toString();
        for (String raw : disabledList(plugin)) {
            if (raw.equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Toggles a player's opt-out state and persists the change.
     *
     * @param plugin   the owning plugin, used to reach the {@code "hpbar"} data store
     * @param playerId the player's unique id
     * @return the new state: {@code true} if now opted out (disabled), {@code false} if now opted back in
     */
    static boolean toggle(SEssentialsPlugin plugin, UUID playerId) {
        YamlStore store = plugin.stores().get("hpbar");
        List<String> disabled = new ArrayList<>(store.config().getStringList(PATH));
        String id = playerId.toString().toLowerCase(Locale.ROOT);

        boolean wasDisabled = disabled.removeIf(raw -> raw.equalsIgnoreCase(id));
        if (!wasDisabled) {
            disabled.add(id);
        }
        store.set(PATH, disabled);
        store.save();
        return !wasDisabled;
    }

    private static List<String> disabledList(SEssentialsPlugin plugin) {
        YamlStore store = plugin.stores().get("hpbar");
        return store.config().getStringList(PATH);
    }
}
