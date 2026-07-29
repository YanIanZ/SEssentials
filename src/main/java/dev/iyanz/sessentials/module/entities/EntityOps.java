package dev.iyanz.sessentials.module.entities;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

/**
 * Whole-world entity operations backing {@link EntitiesModule}: the per-type
 * census and the bulk removal.
 *
 * <p>{@link World#getEntities()} and {@link Entity#remove()} touch a world's
 * entity state, which on Folia must not run from an arbitrary command thread, so
 * every operation is dispatched to the
 * {@link Bukkit#getGlobalRegionScheduler() global region scheduler}. The list
 * returned by {@code getEntities()} is a point-in-time snapshot, which is
 * acceptable for a diagnostic/cleanup command. Results are messaged back on the
 * player sender's own region thread (via {@link Schedulers#entity}) so no
 * cross-region entity access leaks out; console senders are messaged inline.</p>
 */
final class EntityOps {

    /** Maximum number of entity-type rows shown by {@code /entities}. */
    private static final int TOP_LIMIT = 15;

    private EntityOps() {
    }

    /**
     * Counts loaded entities in {@code world} grouped by {@link EntityType} and
     * reports the busiest types (descending) plus the total.
     *
     * @param plugin the owning plugin
     * @param sender the recipient of the report
     * @param world  the world to census
     */
    static void count(SEssentialsPlugin plugin, CommandSender sender, World world) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            Map<EntityType, Integer> counts = new EnumMap<>(EntityType.class);
            int total = 0;
            for (Entity entity : world.getEntities()) {
                counts.merge(entity.getType(), 1, Integer::sum);
                total++;
            }

            List<Map.Entry<EntityType, Integer>> sorted = new ArrayList<>(counts.entrySet());
            sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            int finalTotal = total;

            dispatch(plugin, sender, () -> {
                Msg.value(sender, "Entities in " + world.getName() + ":", String.format(Locale.ROOT, "%,d", finalTotal));
                if (sorted.isEmpty()) {
                    Msg.info(sender, "No entities are loaded in this world.");
                    return;
                }
                int shown = Math.min(sorted.size(), TOP_LIMIT);
                for (int i = 0; i < shown; i++) {
                    Map.Entry<EntityType, Integer> entry = sorted.get(i);
                    Msg.value(sender, " - " + entry.getKey().name().toLowerCase(Locale.ROOT) + ":",
                            String.format(Locale.ROOT, "%,d", entry.getValue()));
                }
                int remaining = sorted.size() - shown;
                if (remaining > 0) {
                    Msg.info(sender, "...and " + remaining + " more entity type" + (remaining == 1 ? "" : "s") + ".");
                }
            });
        });
    }

    /**
     * Removes every entity of {@code type} in {@code world} (players are never
     * removed) and reports how many were cleared.
     *
     * @param plugin the owning plugin
     * @param sender the recipient of the report
     * @param type   the entity type to remove
     * @param world  the world to clear
     */
    static void kill(SEssentialsPlugin plugin, CommandSender sender, EntityType type, World world) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            // Scanning the entity list + reading getType() is a safe read on the global
            // thread, but entity.remove() must run on the entity's OWN region thread on
            // Folia (else "IllegalStateException off the global region"). Collect the
            // matches, then hop each removal to its owner (as HologramRenderer/Butcher do).
            java.util.List<Entity> targets = new java.util.ArrayList<>();
            for (Entity entity : world.getEntities()) {
                if (entity.getType() == type && !(entity instanceof Player)) {
                    targets.add(entity);
                }
            }
            for (Entity target : targets) {
                target.getScheduler().run(plugin, t -> target.remove(), null);
            }
            int found = targets.size();
            String typeName = type.name().toLowerCase(Locale.ROOT);

            dispatch(plugin, sender, () -> {
                if (found == 0) {
                    Msg.info(sender, "No " + typeName + " entities were found in " + world.getName() + ".");
                } else {
                    Msg.ok(sender, "Removed " + found + " " + typeName + " from " + world.getName() + ".");
                }
            });
        });
    }

    /**
     * Delivers a report {@code action}. A player sender is messaged on their own
     * region thread (Folia-safe); any other sender (e.g. console) is messaged inline.
     */
    private static void dispatch(SEssentialsPlugin plugin, CommandSender sender, Runnable action) {
        if (sender instanceof Player player) {
            Schedulers.entity(plugin, player, action);
        } else {
            action.run();
        }
    }
}
