package dev.iyanz.sessentials.module.jail;

import java.util.List;
import java.util.Locale;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.store.YamlStore;
import org.bukkit.Location;

/**
 * Static persistence helpers for named jail locations. Jails are global (server-wide,
 * not per-player) and are kept in the shared {@code jail.yml} data store at
 * {@code "jails.<name>"}, with names always lower-cased so lookups, saves and deletes
 * are case-insensitive. The same store also holds jailed-player state; see
 * {@link JailedPlayers}.
 */
final class Jails {

    private Jails() {
    }

    /** @return the shared jail data store ({@code jail.yml}). */
    static YamlStore store(SEssentialsPlugin plugin) {
        return plugin.stores().get("jail");
    }

    /** @return the storage path for the jail named {@code name} (lower-cased). */
    static String path(String name) {
        return "jails." + name.toLowerCase(Locale.ROOT);
    }

    /** @return all jail names (already lower-case, as stored). */
    static List<String> names(SEssentialsPlugin plugin) {
        return store(plugin).keys("jails");
    }

    /** @return the location of the jail named {@code name}, or {@code null} if unset. */
    static Location location(SEssentialsPlugin plugin, String name) {
        return store(plugin).getLocation(path(name));
    }

    /** @return whether a jail named {@code name} exists. */
    static boolean exists(SEssentialsPlugin plugin, String name) {
        return store(plugin).contains(path(name));
    }

    /** Saves {@code location} as the jail named {@code name} (creating or overwriting it) and persists the store. */
    static void save(SEssentialsPlugin plugin, String name, Location location) {
        YamlStore store = store(plugin);
        store.setLocation(path(name), location);
        store.save();
    }

    /** Deletes the jail named {@code name} and persists the store. */
    static void delete(SEssentialsPlugin plugin, String name) {
        YamlStore store = store(plugin);
        store.remove(path(name));
        store.save();
    }
}
