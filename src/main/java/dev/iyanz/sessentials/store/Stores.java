package dev.iyanz.sessentials.store;

import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.plugin.Plugin;

/**
 * Lazily creates and caches {@link YamlStore}s by file name, so any module can grab a
 * shared data file (e.g. {@code get("homes")}) without coordinating construction.
 */
public final class Stores {

    private final Plugin plugin;
    private final ConcurrentHashMap<String, YamlStore> stores = new ConcurrentHashMap<>();

    public Stores(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * @param name the store name (without extension); {@code "homes"} → {@code homes.yml}
     * @return the shared store for {@code name}, creating it on first use
     */
    public YamlStore get(String name) {
        return stores.computeIfAbsent(name, n -> new YamlStore(plugin, n + ".yml"));
    }
}
