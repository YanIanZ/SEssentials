package dev.iyanz.sessentials.module.warp;

import java.util.List;
import java.util.Locale;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.store.YamlStore;
import org.bukkit.Location;

/**
 * Static persistence helpers for the warp module. Unlike homes, warps are global
 * (server-wide, not per-player) and are kept in the shared {@code warp.yml} data
 * store at {@code "warps.<name>"}, with names always lower-cased so lookups, saves
 * and deletes are case-insensitive.
 */
final class Warps {

    private Warps() {
    }

    /** @return the shared warp data store ({@code warp.yml}). */
    static YamlStore store(SEssentialsPlugin plugin) {
        return plugin.stores().get("warp");
    }

    /** @return the storage path for the warp named {@code name} (lower-cased). */
    static String path(String name) {
        return "warps." + name.toLowerCase(Locale.ROOT);
    }

    /** @return all warp names (already lower-case, as stored). */
    static List<String> names(SEssentialsPlugin plugin) {
        return store(plugin).keys("warps");
    }

    /** @return the location of the warp named {@code name}, or {@code null} if unset. */
    static Location location(SEssentialsPlugin plugin, String name) {
        return store(plugin).getLocation(path(name));
    }

    /** @return whether a warp named {@code name} exists. */
    static boolean exists(SEssentialsPlugin plugin, String name) {
        return store(plugin).contains(path(name));
    }

    /** Saves {@code location} as the warp named {@code name} (creating or overwriting it) and persists the store. */
    static void save(SEssentialsPlugin plugin, String name, Location location) {
        YamlStore store = store(plugin);
        store.setLocation(path(name), location);
        store.save();
    }

    /** Deletes the warp named {@code name} and persists the store. */
    static void delete(SEssentialsPlugin plugin, String name) {
        YamlStore store = store(plugin);
        store.remove(path(name));
        store.save();
    }
}
