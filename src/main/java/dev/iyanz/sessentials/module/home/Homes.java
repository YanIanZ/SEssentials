package dev.iyanz.sessentials.module.home;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.store.YamlStore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Static persistence and limit helpers for the home module. Homes are kept in the
 * shared {@code home.yml} data store at {@code "<uuid>.homes.<name>"}, with names
 * always lower-cased so lookups, saves and deletes are case-insensitive.
 */
final class Homes {

    /**
     * Upper bound scanned when resolving {@code sessentials.homes.<n>} permission
     * tiers (see {@link #maxHomes(SEssentialsPlugin, Player)}). Generous enough for
     * any realistic rank ladder while staying a cheap, bounded scan.
     */
    private static final int MAX_TIER_SCAN = 200;

    private Homes() {
    }

    /** @return the shared home data store ({@code home.yml}). */
    static YamlStore store(SEssentialsPlugin plugin) {
        return plugin.stores().get("home");
    }

    /** @return the storage path for {@code uuid}'s home named {@code name} (lower-cased). */
    static String path(UUID uuid, String name) {
        return uuid + ".homes." + name.toLowerCase(Locale.ROOT);
    }

    /** @return {@code uuid}'s home names (already lower-case, as stored). */
    static List<String> names(SEssentialsPlugin plugin, UUID uuid) {
        return store(plugin).keys(uuid + ".homes");
    }

    /** @return the location of {@code uuid}'s home named {@code name}, or {@code null} if unset. */
    static Location location(SEssentialsPlugin plugin, UUID uuid, String name) {
        return store(plugin).getLocation(path(uuid, name));
    }

    /** @return whether {@code uuid} already has a home named {@code name}. */
    static boolean exists(SEssentialsPlugin plugin, UUID uuid, String name) {
        return store(plugin).contains(path(uuid, name));
    }

    /**
     * Resolves the menu icon for {@code uuid}'s home named {@code name}. If an optional
     * per-home icon material is stored at {@code "<uuid>.home-icons.<name>"} it is used;
     * otherwise (unset or unrecognised) the default {@link Material#RED_BED} is returned.
     * Read-only and additive — the location storage format is untouched.
     *
     * @return the stored icon material, or {@link Material#RED_BED} as a fallback
     */
    static Material icon(SEssentialsPlugin plugin, UUID uuid, String name) {
        String raw = store(plugin).getString(uuid + ".home-icons." + name.toLowerCase(Locale.ROOT));
        Material material = raw == null ? null : Material.matchMaterial(raw);
        return material != null ? material : Material.RED_BED;
    }

    /** Saves {@code location} as {@code uuid}'s home named {@code name} and persists the store. */
    static void save(SEssentialsPlugin plugin, UUID uuid, String name, Location location) {
        YamlStore store = store(plugin);
        store.setLocation(path(uuid, name), location);
        store.save();
    }

    /** Deletes {@code uuid}'s home named {@code name} and persists the store. */
    static void delete(SEssentialsPlugin plugin, UUID uuid, String name) {
        YamlStore store = store(plugin);
        store.remove(path(uuid, name));
        store.save();
    }

    /**
     * Resolves the maximum number of homes {@code player} may own.
     *
     * <p>Starts from the configured {@code home.default-max} (default {@code 3}),
     * overridden by the highest {@code sessentials.homes.<n>} permission tier the
     * player holds (scanned downward from {@link #MAX_TIER_SCAN}), or unlimited
     * (returns {@link Integer#MAX_VALUE}) if the player holds
     * {@code sessentials.homes.unlimited}.</p>
     *
     * @param plugin the owning plugin (for config access)
     * @param player the player to resolve the limit for
     * @return the maximum number of homes, or {@link Integer#MAX_VALUE} if unlimited
     */
    static int maxHomes(SEssentialsPlugin plugin, Player player) {
        if (player.hasPermission("sessentials.homes.unlimited")) {
            return Integer.MAX_VALUE;
        }
        for (int tier = MAX_TIER_SCAN; tier >= 1; tier--) {
            if (player.hasPermission("sessentials.homes." + tier)) {
                return tier;
            }
        }
        return plugin.getConfig().getInt("home.default-max", 3);
    }
}
