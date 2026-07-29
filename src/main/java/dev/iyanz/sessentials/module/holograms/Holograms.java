package dev.iyanz.sessentials.module.holograms;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.store.YamlStore;
import org.bukkit.Location;

/**
 * Static persistence helpers for the holograms module. Holograms are kept in the
 * shared {@code holograms.yml} data store, one top-level section per hologram id:
 * {@code "<id>.location"} (the hologram's location) and {@code "<id>.lines"} (its
 * text lines, each a raw MiniMessage string). Ids are always lower-cased so lookups,
 * saves and deletes are case-insensitive.
 */
final class Holograms {

    private Holograms() {
    }

    /** @return the shared holograms data store ({@code holograms.yml}). */
    static YamlStore store(SEssentialsPlugin plugin) {
        return plugin.stores().get("holograms");
    }

    /** @return {@code id}, lower-cased for case-insensitive storage. */
    static String key(String id) {
        return id.toLowerCase(Locale.ROOT);
    }

    /** @return every hologram id (already lower-case, as stored). */
    static List<String> ids(SEssentialsPlugin plugin) {
        return List.copyOf(store(plugin).config().getKeys(false));
    }

    /** @return whether a hologram named {@code id} exists. */
    static boolean exists(SEssentialsPlugin plugin, String id) {
        return store(plugin).contains(id + ".location");
    }

    /** @return the location of the hologram named {@code id}, or {@code null} if unset. */
    static Location location(SEssentialsPlugin plugin, String id) {
        return store(plugin).getLocation(id + ".location");
    }

    /** @return the text lines (raw MiniMessage) of the hologram named {@code id}. */
    static List<String> lines(SEssentialsPlugin plugin, String id) {
        return new ArrayList<>(store(plugin).config().getStringList(id + ".lines"));
    }

    /**
     * Creates a brand-new hologram at {@code location} with a single line and persists
     * the store.
     */
    static void create(SEssentialsPlugin plugin, String id, Location location, String firstLine) {
        YamlStore store = store(plugin);
        store.setLocation(id + ".location", location);
        store.set(id + ".lines", List.of(firstLine));
        store.save();
    }

    /** Deletes the hologram named {@code id} (its location and lines) and persists the store. */
    static void delete(SEssentialsPlugin plugin, String id) {
        YamlStore store = store(plugin);
        store.remove(id);
        store.save();
    }

    /** Moves the hologram named {@code id} to {@code location} and persists the store. */
    static void setLocation(SEssentialsPlugin plugin, String id, Location location) {
        YamlStore store = store(plugin);
        store.setLocation(id + ".location", location);
        store.save();
    }

    /** Overwrites the hologram named {@code id}'s text lines and persists the store. */
    static void setLines(SEssentialsPlugin plugin, String id, List<String> lines) {
        YamlStore store = store(plugin);
        store.set(id + ".lines", lines);
        store.save();
    }
}
