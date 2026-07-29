package dev.iyanz.sessentials.module.signs;

import java.util.Locale;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.store.YamlStore;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.potion.PotionEffectType;

/**
 * Executes the effect of each {@link SignTag} for a player who right-clicked a valid,
 * permitted action sign. Every method here is expected to already run on the clicking
 * player's region thread (see {@link SignInteractListener}).
 */
final class SignActions {

    private static final String WARP_STORE = "warp";
    private static final String SPAWN_STORE = "spawn";
    private static final String SPAWN_PATH = "spawn";
    private static final float FULL_FOOD_SATURATION = 20f;
    private static final int FULL_FOOD_LEVEL = 20;

    private SignActions() {
    }

    /**
     * Runs the action for {@code tag} on {@code player}.
     *
     * @param plugin   the owning plugin (data stores)
     * @param player   the clicker
     * @param tag      the matched action-sign type
     * @param argument the sign's second line (trimmed), used by {@code warp}, {@code command} and {@code kit}
     */
    static void run(SEssentialsPlugin plugin, Player player, SignTag tag, String argument) {
        switch (tag) {
            case WARP -> warp(plugin, player, argument);
            case SPAWN -> spawn(plugin, player);
            case COMMAND -> command(player, argument);
            case HEAL -> heal(player);
            case FEED -> feed(player);
            case KIT -> kit(player, argument);
        }
    }

    /** Teleports {@code player} to the warp named {@code name}, or errors if it isn't set. */
    private static void warp(SEssentialsPlugin plugin, Player player, String name) {
        if (name.isEmpty()) {
            Msg.err(player, "This warp sign has no warp name set.");
            return;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        YamlStore store = plugin.stores().get(WARP_STORE);
        Location location = store.getLocation("warps." + lower);
        if (location == null) {
            Msg.err(player, "No warp named \"" + lower + "\".");
            return;
        }
        player.teleportAsync(location).thenAccept(success -> {
            if (Boolean.TRUE.equals(success)) {
                Msg.ok(player, "Warped to \"" + lower + "\".");
            } else {
                Msg.err(player, "Teleport to warp \"" + lower + "\" failed.");
            }
        });
    }

    /** Teleports {@code player} to the configured server spawn, falling back to the world's vanilla spawn. */
    private static void spawn(SEssentialsPlugin plugin, Player player) {
        YamlStore store = plugin.stores().get(SPAWN_STORE);
        Location location = store.getLocation(SPAWN_PATH);
        if (location == null) {
            location = player.getWorld().getSpawnLocation();
        }
        player.teleportAsync(location, TeleportCause.PLUGIN).thenAccept(success -> {
            if (Boolean.TRUE.equals(success)) {
                Msg.ok(player, "Teleported to spawn.");
            } else {
                Msg.err(player, "Teleport to spawn failed.");
            }
        });
    }

    /** Runs {@code command} (no leading {@code /}) as {@code player}. */
    private static void command(Player player, String command) {
        if (command.isEmpty()) {
            Msg.err(player, "This command sign has no command set.");
            return;
        }
        player.performCommand(command);
    }

    /** Fully heals {@code player}: max health, full hunger, and clears fire/poison/wither. */
    private static void heal(Player player) {
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(FULL_FOOD_LEVEL);
        player.setFireTicks(0);
        player.removePotionEffect(PotionEffectType.POISON);
        player.removePotionEffect(PotionEffectType.WITHER);
        Msg.ok(player, "You have been healed.");
    }

    /** Refills {@code player}'s hunger bar and saturation. */
    private static void feed(Player player) {
        player.setFoodLevel(FULL_FOOD_LEVEL);
        player.setSaturation(FULL_FOOD_SATURATION);
        Msg.ok(player, "You have been fed.");
    }

    /** Grants the kit named {@code name} to {@code player} via {@code /kit <name>}. */
    private static void kit(Player player, String name) {
        if (name.isEmpty()) {
            Msg.err(player, "This kit sign has no kit name set.");
            return;
        }
        player.performCommand("kit " + name);
    }
}
