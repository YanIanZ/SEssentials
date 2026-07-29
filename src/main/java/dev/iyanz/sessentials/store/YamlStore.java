package dev.iyanz.sessentials.store;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
 *
 * <p><strong>Thread-safety.</strong> On Folia a single {@code YamlStore} is shared by
 * every region thread, and the backing {@link FileConfiguration} is a plain
 * {@code LinkedHashMap}-backed structure that is not safe for concurrent access. Two
 * region threads mutating the same store — or one mutating it while the async
 * {@link #save()} serialises it — previously raced, causing
 * {@code ConcurrentModificationException} and lost writes. Every accessor here now holds
 * a single monitor, and {@link #save()} serialises the config to a string <em>while
 * holding that monitor on the calling thread</em> and only the disk write runs async, so
 * serialisation can never observe a half-applied mutation.</p>
 *
 * <p>Callers that reach the raw {@link #config()} bypass this monitor; prefer the typed
 * accessors ({@link #getString}, {@link #getStringList}, {@link #set}, {@link #remove},
 * {@link #keys}, …) for any state that is written from more than one thread.</p>
 */
public final class YamlStore {

    private final Plugin plugin;
    private final File file;
    private final FileConfiguration config;
    /** Single monitor guarding all reads, writes and serialisation of {@link #config}. */
    private final Object lock = new Object();

    YamlStore(Plugin plugin, String fileName) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), fileName);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * @return the underlying configuration for advanced access. <strong>Not
     *         monitor-guarded</strong> — only use for read-only or single-threaded access;
     *         for concurrently-written state use the typed accessors on this class.
     */
    public FileConfiguration config() {
        return config;
    }

    public boolean contains(String path) {
        synchronized (lock) {
            return config.contains(path);
        }
    }

    public String getString(String path) {
        synchronized (lock) {
            return config.getString(path);
        }
    }

    /** @return the string list at {@code path} (a defensive copy), never {@code null}. */
    public List<String> getStringList(String path) {
        synchronized (lock) {
            return List.copyOf(config.getStringList(path));
        }
    }

    /** @return the {@code long} at {@code path}, or {@code def} if absent. */
    public long getLong(String path, long def) {
        synchronized (lock) {
            return config.getLong(path, def);
        }
    }

    public void set(String path, Object value) {
        synchronized (lock) {
            config.set(path, value);
        }
    }

    public List<String> keys(String path) {
        synchronized (lock) {
            var section = config.getConfigurationSection(path);
            return section == null ? List.of() : List.copyOf(section.getKeys(false));
        }
    }

    /** Stores a location under {@code path} (encoded). */
    public void setLocation(String path, Location location) {
        synchronized (lock) {
            config.set(path, Locations.toString(location));
        }
    }

    /** @return the location stored at {@code path}, or {@code null}. */
    public Location getLocation(String path) {
        synchronized (lock) {
            return Locations.fromString(config.getString(path));
        }
    }

    /** Removes {@code path}. */
    public void remove(String path) {
        synchronized (lock) {
            config.set(path, null);
        }
    }

    /**
     * Saves the file asynchronously (safe on Folia). The config is serialised to a string
     * on the calling thread while holding the store monitor — so it cannot race a
     * concurrent mutation — and only the disk write is offloaded to an async task.
     */
    public void save() {
        final String data;
        synchronized (lock) {
            data = config.saveToString();
        }
        Schedulers.async(plugin, t -> {
            try {
                Files.writeString(file.toPath(), data, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                plugin.getLogger().warning("Failed to save " + file.getName() + ": " + ex.getMessage());
            }
        });
    }
}
