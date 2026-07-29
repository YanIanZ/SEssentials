package dev.iyanz.sessentials.module.counter;

import java.util.List;
import java.util.Locale;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.store.YamlStore;

/**
 * Static persistence helpers for the counter module. Each counter is a single named
 * integer stored at the top level of the shared {@code counters.yml} data store
 * (path {@code "<name>"}), with names always lower-cased so lookups are
 * case-insensitive. A counter that has never been set reads as {@code 0}.
 */
final class Counters {

    private Counters() {
    }

    /** @return the shared counter data store ({@code counters.yml}). */
    private static YamlStore store(SEssentialsPlugin plugin) {
        return plugin.stores().get("counters");
    }

    /** @return the storage path for the counter named {@code name} (lower-cased). */
    private static String path(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    /** @return all known counter names (already lower-case, as stored), in no particular order. */
    static List<String> names(SEssentialsPlugin plugin) {
        return store(plugin).keys("");
    }

    /** @return the current value of the counter named {@code name}, or {@code 0} if never set. */
    static int get(SEssentialsPlugin plugin, String name) {
        return store(plugin).config().getInt(path(name), 0);
    }

    /** Sets the counter named {@code name} to {@code value} and persists the store. */
    static void set(SEssentialsPlugin plugin, String name, int value) {
        YamlStore store = store(plugin);
        store.set(path(name), value);
        store.save();
    }
}
