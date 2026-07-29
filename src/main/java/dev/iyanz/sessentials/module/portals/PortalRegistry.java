package dev.iyanz.sessentials.module.portals;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.store.YamlStore;
import org.bukkit.Location;

/**
 * In-memory registry of all portals, backed by the shared {@code portals.yml} data
 * store. Each portal is stored as a top-level section named after the portal, with
 * children {@code world}, {@code min}, {@code max} (each a {@code "x,y,z"} string) and
 * {@code dest} (see {@link Portal}).
 *
 * <p>The registry is loaded once via {@link #load(SEssentialsPlugin)} on module enable
 * and kept in sync in memory on every {@link #save(SEssentialsPlugin, Portal)} /
 * {@link #delete(SEssentialsPlugin, String)}, so the frequent per-move containment
 * check ({@link #find(Location)}) never touches disk.</p>
 */
final class PortalRegistry {

    private final Map<String, Portal> cache = new ConcurrentHashMap<>();

    /** @return the shared portal data store ({@code portals.yml}). */
    private static YamlStore store(SEssentialsPlugin plugin) {
        return plugin.stores().get("portals");
    }

    /**
     * (Re)loads every portal from disk into memory, replacing the current cache.
     * Malformed entries (missing/unparsable fields) are skipped.
     *
     * @param plugin the owning plugin
     */
    void load(SEssentialsPlugin plugin) {
        cache.clear();
        YamlStore store = store(plugin);
        for (String name : store.keys("")) {
            Portal portal = read(store, name);
            if (portal != null) {
                cache.put(name, portal);
            }
        }
    }

    /** @return the portal named {@code name} (already lower-case), or {@code null} if unset. */
    Portal get(String name) {
        return cache.get(name);
    }

    /** @return whether a portal named {@code name} exists. */
    boolean exists(String name) {
        return cache.containsKey(name);
    }

    /** @return all portal names (lower-case, as stored), in no particular order. */
    List<String> names() {
        return List.copyOf(cache.keySet());
    }

    /** @return all portals currently known, in no particular order. */
    Collection<Portal> all() {
        return List.copyOf(cache.values());
    }

    /**
     * @param location the location to test
     * @return the first portal whose bounding box contains {@code location}, or
     *         {@code null} if none match
     */
    Portal find(Location location) {
        for (Portal portal : cache.values()) {
            if (portal.contains(location)) {
                return portal;
            }
        }
        return null;
    }

    /** Saves {@code portal} (creating or overwriting it), persisting the store and updating the cache. */
    void save(SEssentialsPlugin plugin, Portal portal) {
        YamlStore store = store(plugin);
        store.set(portal.name() + ".world", portal.world());
        store.set(portal.name() + ".min", encode(portal.minX(), portal.minY(), portal.minZ()));
        store.set(portal.name() + ".max", encode(portal.maxX(), portal.maxY(), portal.maxZ()));
        store.set(portal.name() + ".dest", portal.destination());
        store.save();
        cache.put(portal.name(), portal);
    }

    /** Deletes the portal named {@code name}, persisting the store and updating the cache. */
    void delete(SEssentialsPlugin plugin, String name) {
        YamlStore store = store(plugin);
        store.remove(name);
        store.save();
        cache.remove(name);
    }

    private static Portal read(YamlStore store, String name) {
        String world = store.getString(name + ".world");
        String destination = store.getString(name + ".dest");
        double[] min = decode(store.getString(name + ".min"));
        double[] max = decode(store.getString(name + ".max"));
        if (world == null || destination == null || min == null || max == null) {
            return null;
        }
        return new Portal(name, world, min[0], min[1], min[2], max[0], max[1], max[2], destination);
    }

    /** @return {@code "x,y,z"}, encoding the three coordinates for storage. */
    private static String encode(double x, double y, double z) {
        return x + "," + y + "," + z;
    }

    /** @return the decoded {@code {x, y, z}} triple from an {@link #encode} string, or {@code null} if malformed. */
    private static double[] decode(String encoded) {
        if (encoded == null) {
            return null;
        }
        String[] parts = encoded.split(",");
        if (parts.length != 3) {
            return null;
        }
        try {
            return new double[]{
                    Double.parseDouble(parts[0].trim()),
                    Double.parseDouble(parts[1].trim()),
                    Double.parseDouble(parts[2].trim())
            };
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
