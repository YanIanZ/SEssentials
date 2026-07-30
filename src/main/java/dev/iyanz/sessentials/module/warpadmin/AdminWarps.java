package dev.iyanz.sessentials.module.warpadmin;

import java.util.List;
import java.util.Locale;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.store.YamlStore;
import org.bukkit.Location;
import org.bukkit.Material;

/**
 * Read-only view of the shared warp data store for the warp-admin GUI.
 *
 * <p>The warp module keeps its own persistence in the package-private
 * {@code dev.iyanz.sessentials.module.warp.Warps}, which this module cannot reach. Rather than
 * couple the two packages, this helper re-derives the same storage convention — the {@code warp}
 * store ({@code warp.yml}), locations at {@code warps.<name>} and optional icon materials at
 * {@code warp-icons.<name>}, names always lower-cased — and reads it <strong>without ever
 * writing</strong>. Every warp mutation the GUI performs is delegated to the audited
 * {@code /setwarp} and {@code /delwarp} commands, so this class is deliberately read-only.</p>
 */
final class AdminWarps {

    private AdminWarps() {
    }

    /** @return the shared warp data store ({@code warp.yml}); the same file the warp module uses. */
    private static YamlStore store(SEssentialsPlugin plugin) {
        return plugin.stores().get("warp");
    }

    /** @return all warp names (already lower-case, as stored). */
    static List<String> names(SEssentialsPlugin plugin) {
        return store(plugin).keys("warps");
    }

    /** @return the location of the warp named {@code name}, or {@code null} if unset. */
    static Location location(SEssentialsPlugin plugin, String name) {
        return store(plugin).getLocation("warps." + name.toLowerCase(Locale.ROOT));
    }

    /**
     * Resolves the menu icon for the warp named {@code name}: the material stored at
     * {@code "warp-icons.<name>"} if present and recognised, otherwise {@link Material#ENDER_PEARL}.
     *
     * @return the stored icon material, or {@link Material#ENDER_PEARL} as a fallback
     */
    static Material icon(SEssentialsPlugin plugin, String name) {
        String raw = store(plugin).getString("warp-icons." + name.toLowerCase(Locale.ROOT));
        Material material = raw == null ? null : Material.matchMaterial(raw);
        return material != null ? material : Material.ENDER_PEARL;
    }
}
