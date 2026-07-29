package dev.iyanz.sessentials.module.worlds;

import java.io.File;
import java.util.List;
import java.util.Locale;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Operational logic behind {@code /world}: listing, teleporting, creating,
 * loading, unloading, spawn management and gamerules.
 *
 * <p>World creation/loading/unloading are heavy, whole-server operations, so they
 * run on Folia's {@link Bukkit#getGlobalRegionScheduler() global region scheduler}.
 * Spawn and gamerule mutations touch a single world's state and run on that
 * world's own region thread via {@link Bukkit#getRegionScheduler()}.</p>
 */
final class WorldOps {

    private WorldOps() {
    }

    /** Lists every loaded world with its player count and environment. */
    static void list(SEssentialsPlugin plugin, CommandSender sender) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            List<World> worlds = Bukkit.getWorlds();
            if (worlds.isEmpty()) {
                Msg.info(sender, "No worlds are loaded.");
                return;
            }
            Msg.info(sender, "Loaded worlds (" + worlds.size() + "):");
            for (World world : worlds) {
                Msg.value(sender, " - " + world.getName() + " (" + world.getEnvironment() + "):",
                        world.getPlayers().size() + " players");
            }
        });
    }

    /** Teleports {@code player} to {@code worldName}'s spawn point. */
    static int teleport(Player player, String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            Msg.err(player, "No loaded world named \"" + worldName + "\".");
            return 0;
        }
        player.teleportAsync(world.getSpawnLocation()).thenAccept(success -> {
            if (Boolean.TRUE.equals(success)) {
                Msg.ok(player, "Teleported to \"" + world.getName() + "\".");
            } else {
                Msg.err(player, "Teleport to \"" + world.getName() + "\" failed.");
            }
        });
        return 1;
    }

    /**
     * Creates and loads a brand-new world.
     *
     * @param environmentName {@code NORMAL}/{@code NETHER}/{@code THE_END}, or {@code null} for {@code NORMAL}
     * @param rawSeed         a numeric or text seed, or {@code null} for a random seed
     */
    static int create(SEssentialsPlugin plugin, CommandSender sender, String name, String environmentName, String rawSeed) {
        if (Bukkit.getWorld(name) != null) {
            Msg.err(sender, "A world named \"" + name + "\" is already loaded.");
            return 0;
        }
        World.Environment environment = World.Environment.NORMAL;
        if (environmentName != null) {
            try {
                environment = World.Environment.valueOf(environmentName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                Msg.err(sender, "Unknown environment: " + environmentName + ". Use NORMAL, NETHER or THE_END.");
                return 0;
            }
        }
        WorldCreator creator = new WorldCreator(name).environment(environment);
        if (rawSeed != null) {
            creator.seed(parseSeed(rawSeed));
        }
        World.Environment finalEnvironment = environment;
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            World created = Bukkit.createWorld(creator);
            if (created == null) {
                Msg.err(sender, "Failed to create world \"" + name + "\".");
            } else {
                Msg.ok(sender, "Created world \"" + created.getName() + "\" (" + finalEnvironment + ").");
            }
        });
        return 1;
    }

    /** Loads an existing world folder that isn't currently loaded. */
    static int load(SEssentialsPlugin plugin, CommandSender sender, String name) {
        if (Bukkit.getWorld(name) != null) {
            Msg.err(sender, "World \"" + name + "\" is already loaded.");
            return 0;
        }
        File folder = new File(Bukkit.getWorldContainer(), name);
        if (!folder.isDirectory()) {
            Msg.err(sender, "No world folder named \"" + name + "\" was found.");
            return 0;
        }
        WorldCreator creator = new WorldCreator(name);
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            World loaded = Bukkit.createWorld(creator);
            if (loaded == null) {
                Msg.err(sender, "Failed to load world \"" + name + "\".");
            } else {
                Msg.ok(sender, "Loaded world \"" + loaded.getName() + "\".");
            }
        });
        return 1;
    }

    /** Unloads a loaded world, refusing to unload the default/overworld. */
    static int unload(SEssentialsPlugin plugin, CommandSender sender, String name) {
        World world = Bukkit.getWorld(name);
        if (world == null) {
            Msg.err(sender, "No loaded world named \"" + name + "\".");
            return 0;
        }
        World defaultWorld = Bukkit.getWorlds().get(0);
        if (world.equals(defaultWorld)) {
            Msg.err(sender, "Cannot unload the default world.");
            return 0;
        }
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            boolean unloaded = Bukkit.unloadWorld(world, true);
            if (unloaded) {
                Msg.ok(sender, "Unloaded world \"" + name + "\".");
            } else {
                Msg.err(sender, "Failed to unload world \"" + name + "\" (players may still be inside).");
            }
        });
        return 1;
    }

    /** Sets {@code player}'s current world's spawn point to their current location. */
    static int setSpawn(SEssentialsPlugin plugin, Player player) {
        World world = player.getWorld();
        Location location = player.getLocation();
        Bukkit.getRegionScheduler().execute(plugin, world, 0, 0, () -> {
            boolean applied = world.setSpawnLocation(location);
            if (applied) {
                Msg.ok(player, "Spawn for \"" + world.getName() + "\" set to your location.");
            } else {
                Msg.err(player, "Failed to set spawn for \"" + world.getName() + "\".");
            }
        });
        return 1;
    }

    /**
     * Applies a gamerule change.
     *
     * @param worldName the target world, or {@code null} to use the sender's current world
     *                   (only valid for a player sender)
     */
    static int gameRule(SEssentialsPlugin plugin, CommandSender sender, String ruleName, String value, String worldName) {
        GameRule<?> rule = GameRule.getByName(ruleName);
        if (rule == null) {
            Msg.err(sender, "Unknown gamerule: " + ruleName);
            return 0;
        }
        World world;
        if (worldName != null) {
            world = Bukkit.getWorld(worldName);
            if (world == null) {
                Msg.err(sender, "No loaded world named \"" + worldName + "\".");
                return 0;
            }
        } else if (sender instanceof Player player) {
            world = player.getWorld();
        } else {
            Msg.err(sender, "Console must specify a world: /world gamerule <rule> <value> <world>.");
            return 0;
        }
        World targetWorld = world;
        Bukkit.getRegionScheduler().execute(plugin, targetWorld, 0, 0, () -> {
            boolean applied = targetWorld.setGameRuleValue(rule.getName(), value);
            if (applied) {
                Msg.value(sender, "Gamerule " + rule.getName() + " (" + targetWorld.getName() + ") set to", value);
            } else {
                Msg.err(sender, "Invalid value \"" + value + "\" for gamerule " + rule.getName() + ".");
            }
        });
        return 1;
    }

    /** A numeric seed is parsed as-is; any other text seed is hashed into a long, like vanilla does. */
    private static long parseSeed(String rawSeed) {
        try {
            return Long.parseLong(rawSeed);
        } catch (NumberFormatException ex) {
            return rawSeed.hashCode();
        }
    }
}
