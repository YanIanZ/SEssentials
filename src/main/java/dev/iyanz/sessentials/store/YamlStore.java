package dev.iyanz.sessentials.store;

import java.io.File;
import java.io.IOException;
import java.util.List;

import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Locations;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * A YAML-backed data file (e.g. {@code homes.yml}, {@code warps.yml}) with convenient
 * typed accessors and asynchronous, Folia-safe saving. Reads are in-memory; writes
 * mutate the in-memory config and schedule a debounced save.
 */
public final class YamlStore {

    private final Plugin plugin;
    private final File file;
    private final FileConfiguration config;
    private final Object saveLock = new Object();

    YamlStore(Plugin plugin, String fileName) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), fileName);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    /** @return the underlying configuration for advanced access. */
    public FileConfiguration config() {
        return config;
    }

    public boolean contains(String path) {
        return config.contains(path);
    }

    public String getString(String path) {
        return config.getString(path);
    }

    public void set(String path, Object value) {
        config.set(path, value);
    }

    public List<String> keys(String path) {
        var section = config.getConfigurationSection(path);
        return section == null ? List.of() : List.copyOf(section.getKeys(false));
    }

    /** Stores a location under {@code path} (encoded). */
    public void setLocation(String path, Location location) {
        config.set(path, Locations.toString(location));
    }

    /** @return the location stored at {@code path}, or {@code null}. */
    public Location getLocation(String path) {
        return Locations.fromString(config.getString(path));
    }

    /** Removes {@code path}. */
    public void remove(String path) {
        config.set(path, null);
    }

    /** Saves the file asynchronously (safe on Folia). */
    public void save() {
        Schedulers.async(plugin, t -> {
            synchronized (saveLock) {
                try {
                    config.save(file);
                } catch (IOException ex) {
                    plugin.getLogger().warning("Failed to save " + file.getName() + ": " + ex.getMessage());
                }
            }
        });
    }
}
