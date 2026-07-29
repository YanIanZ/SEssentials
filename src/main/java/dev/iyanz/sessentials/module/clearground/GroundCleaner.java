package dev.iyanz.sessentials.module.clearground;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.SmallCaps;
import dev.iyanz.sessentials.util.Style;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Folia-safe ground-clearing routines shared by {@link ClearGroundModule}: resolving
 * the target world, scheduling the warned/immediate sweeps, removing dropped items and
 * announcing the result.
 *
 * <p>Every world sweep runs on the {@link Bukkit#getGlobalRegionScheduler() global
 * region scheduler}: {@link #clearNow} executes it at once, while {@link #scheduleWarned}
 * broadcasts a warning and defers the sweep by {@link #WARNING_TICKS} ticks. Broadcasts
 * hop to each recipient's own region thread via {@link Schedulers#entity}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class GroundCleaner {

    /** Grace period, in seconds, between the warning and the warned sweep. */
    private static final int WARNING_SECONDS = 10;

    /** The warned-sweep delay in ticks (20 ticks per second). */
    private static final long WARNING_TICKS = WARNING_SECONDS * 20L;

    /** Tab-completes the names of every currently loaded world, filtered by prefix. */
    static final SuggestionProvider<CommandSourceStack> WORLDS = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (World world : Bukkit.getWorlds()) {
            if (world.getName().toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(world.getName());
            }
        }
        return builder.buildFuture();
    };

    private GroundCleaner() {
    }

    /**
     * Resolves the world a command should act on.
     *
     * @param sender    the command sender
     * @param worldName the requested world name, or {@code null} to use the sender's world
     * @return the matching {@link World}, or {@code null} after messaging {@code sender}
     *         when the name is unknown or a console sender named no world
     */
    static World resolveWorld(CommandSender sender, String worldName) {
        if (worldName != null) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                Msg.err(sender, "Unknown world: " + worldName + ".");
            }
            return world;
        }
        if (sender instanceof Player player) {
            return player.getWorld();
        }
        Msg.err(sender, "Specify a world from the console.");
        return null;
    }

    /**
     * Broadcasts a warning, then sweeps {@code world}'s dropped items after the grace
     * period, announcing how many were removed.
     *
     * @param plugin the owning plugin
     * @param world  the world to clear
     */
    static void scheduleWarned(SEssentialsPlugin plugin, World world) {
        broadcast(plugin, warning());
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> sweepAndAnnounce(plugin, world), WARNING_TICKS);
    }

    /**
     * Sweeps {@code world}'s dropped items immediately, announcing how many were removed.
     *
     * @param plugin the owning plugin
     * @param world  the world to clear
     */
    static void clearNow(SEssentialsPlugin plugin, World world) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> sweepAndAnnounce(plugin, world));
    }

    /** Removes every dropped item in {@code world} and broadcasts the count. */
    private static void sweepAndAnnounce(SEssentialsPlugin plugin, World world) {
        int removed = removeDroppedItems(world);
        broadcast(plugin, cleared(world, removed));
    }

    /** Removes every dropped {@link Item} entity in {@code world}. */
    private static int removeDroppedItems(World world) {
        int removed = 0;
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Item item) {
                item.remove();
                removed++;
            }
        }
        return removed;
    }

    /** Sends {@code message} to the console and to every online player, each on its own region thread. */
    private static void broadcast(SEssentialsPlugin plugin, Component message) {
        Bukkit.getConsoleSender().sendMessage(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            Schedulers.entity(plugin, player, () -> player.sendMessage(message));
        }
    }

    /** @return the pre-sweep warning line. */
    private static Component warning() {
        return Msg.mm(Msg.prefix() + Style.HINT
                + SmallCaps.of("Ground items will be cleared in " + WARNING_SECONDS + " seconds!"));
    }

    /** @return the post-sweep summary line for {@code world}. */
    private static Component cleared(World world, int removed) {
        return Msg.mm(Msg.prefix()
                + Style.OK + SmallCaps.of("Cleared ")
                + Style.COIN + removed + " "
                + Style.OK + SmallCaps.of("ground items in ")
                + Style.VALUE + world.getName()
                + Style.OK + SmallCaps.of("."));
    }
}
