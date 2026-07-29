package dev.iyanz.sessentials.module.holograms;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;

/**
 * Owns the live {@link TextDisplay} entities that render holograms, and keeps every
 * mutation Folia-safe by routing entity/world work to the region thread that owns the
 * relevant location.
 *
 * <p>Each rendered hologram is a single {@link TextDisplay} tagged with a
 * {@code sessentials:hologram_id} persistent-data entry (the hologram's id), so stray
 * entities (e.g. left over from an unclean shutdown) can always be identified and
 * swept up. An in-memory {@code id -> entity UUID} map avoids re-scanning a world's
 * entities on every edit.</p>
 */
final class HologramRenderer {

    private static final String TAG_NAME = "hologram_id";
    private static final ConcurrentHashMap<String, UUID> ACTIVE = new ConcurrentHashMap<>();

    private HologramRenderer() {
    }

    /** @return the {@code sessentials:hologram_id} tag key, scoped to {@code plugin}. */
    private static NamespacedKey tagKey(SEssentialsPlugin plugin) {
        return new NamespacedKey(plugin, TAG_NAME);
    }

    /**
     * Called once on module enable. For every currently loaded world, removes any
     * stale tagged {@link TextDisplay}s (left behind by a previous run) and then
     * (re)spawns every hologram persisted for that world.
     *
     * @param plugin the owning plugin
     */
    static void loadAll(SEssentialsPlugin plugin) {
        ACTIVE.clear();
        for (World world : Bukkit.getWorlds()) {
            cleanupAndSpawn(plugin, world);
        }
    }

    /** Removes every tagged {@link TextDisplay} across every loaded world. Called on module disable. */
    static void unloadAll(SEssentialsPlugin plugin) {
        NamespacedKey tag = tagKey(plugin);
        for (World world : Bukkit.getWorlds()) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, () -> removeTagged(plugin, world, tag));
        }
        ACTIVE.clear();
    }

    /**
     * Spawns a fresh, tagged {@link TextDisplay} for hologram {@code id} at
     * {@code location}, dispatched onto that location's own region thread. Replaces
     * the tracked entity for {@code id}, if any.
     */
    static void spawn(SEssentialsPlugin plugin, String id, Location location, List<String> lines) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        Bukkit.getRegionScheduler().execute(plugin, world, location.getBlockX() >> 4, location.getBlockZ() >> 4,
                () -> {
                    TextDisplay display = world.spawn(location, TextDisplay.class,
                            td -> configure(plugin, td, id, lines));
                    ACTIVE.put(id, display.getUniqueId());
                });
    }

    /**
     * Removes the tracked entity for hologram {@code id}, dispatched onto
     * {@code currentLocation}'s region thread (where the live entity is expected to
     * be). Safe to call even if the entity is already gone.
     */
    static void despawn(SEssentialsPlugin plugin, String id, Location currentLocation) {
        UUID uuid = ACTIVE.remove(id);
        if (currentLocation == null) {
            return;
        }
        World world = currentLocation.getWorld();
        if (world == null) {
            return;
        }
        Bukkit.getRegionScheduler().execute(plugin, world,
                currentLocation.getBlockX() >> 4, currentLocation.getBlockZ() >> 4, () -> {
                    Entity entity = uuid == null ? null : world.getEntity(uuid);
                    if (entity != null) {
                        entity.remove();
                    }
                });
    }

    /**
     * Moves hologram {@code id} from {@code oldLocation} to {@code newLocation}: removes
     * the entity at its old region, then spawns a fresh one at the new region (a
     * relocation may cross Folia regions, so respawning is simpler and safer than an
     * in-place teleport).
     */
    static void relocate(SEssentialsPlugin plugin, String id, Location oldLocation, Location newLocation,
            List<String> lines) {
        despawn(plugin, id, oldLocation);
        spawn(plugin, id, newLocation, lines);
    }

    /**
     * Refreshes the displayed text of hologram {@code id} in place, dispatched onto
     * {@code location}'s region thread. If the tracked entity can't be found (e.g. it
     * was never spawned, or was removed out-of-band), spawns a fresh one instead.
     */
    static void updateText(SEssentialsPlugin plugin, String id, Location location, List<String> lines) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        UUID uuid = ACTIVE.get(id);
        Bukkit.getRegionScheduler().execute(plugin, world, location.getBlockX() >> 4, location.getBlockZ() >> 4,
                () -> {
                    Entity entity = uuid == null ? null : world.getEntity(uuid);
                    if (entity instanceof TextDisplay display && !display.isDead()) {
                        display.text(renderText(lines));
                    } else {
                        spawn(plugin, id, location, lines);
                    }
                });
    }

    /** Removes stale tagged entities in {@code world}, then spawns every hologram persisted for it. */
    private static void cleanupAndSpawn(SEssentialsPlugin plugin, World world) {
        NamespacedKey tag = tagKey(plugin);
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            removeTagged(plugin, world, tag);
            for (String id : Holograms.ids(plugin)) {
                Location location = Holograms.location(plugin, id);
                if (location == null || location.getWorld() == null || !location.getWorld().equals(world)) {
                    continue;
                }
                spawn(plugin, id, location, Holograms.lines(plugin, id));
            }
        });
    }

    /**
     * Removes every entity in {@code world} tagged with {@code tag}. Each removal is
     * dispatched onto the offending entity's <em>own</em> region thread via its
     * {@link Entity#getScheduler() entity scheduler}, so a tagged display far from the
     * origin (owned by a different Folia region) is removed on the thread that owns it
     * rather than tripping the thread-ownership check. The just-spawned fresh displays
     * from {@link #cleanupAndSpawn} do not yet exist during this scan, so they are never
     * swept away.
     */
    private static void removeTagged(SEssentialsPlugin plugin, World world, NamespacedKey tag) {
        for (Entity entity : world.getEntities()) {
            if (entity instanceof TextDisplay && entity.getPersistentDataContainer().has(tag, PersistentDataType.STRING)) {
                entity.getScheduler().run(plugin, task -> entity.remove(), null);
            }
        }
    }

    /** Configures a freshly spawned {@link TextDisplay}: billboard, text and the id tag. Must run on its region thread. */
    private static void configure(SEssentialsPlugin plugin, TextDisplay display, String id, List<String> lines) {
        display.setBillboard(Display.Billboard.CENTER);
        display.text(renderText(lines));
        display.getPersistentDataContainer().set(tagKey(plugin), PersistentDataType.STRING, id);
    }

    /** @return {@code lines} joined with MiniMessage {@code <newline>} and deserialized. */
    private static Component renderText(List<String> lines) {
        return Msg.mm(String.join("<newline>", lines));
    }
}
