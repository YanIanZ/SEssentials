package dev.iyanz.sessentials.module.teleport;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.store.YamlStore;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/**
 * Registers {@code /spawn} and {@code /setspawn}. The global spawn location is
 * persisted in the {@code spawn} data store at path {@code "spawn"}; if none is set,
 * {@code /spawn} falls back to the current world's vanilla spawn.
 */
@SuppressWarnings("UnstableApiUsage")
final class SpawnCommands {

    private static final String STORE_NAME = "spawn";
    private static final String PATH = "spawn";

    private SpawnCommands() {
    }

    /**
     * Registers the {@code /spawn} and {@code /setspawn} commands.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            reg.register(Commands.literal("spawn")
                    .requires(s -> s.getSender().hasPermission("sessentials.spawn"))
                    .executes(Cmds.playerExec(player -> {
                        YamlStore store = plugin.stores().get(STORE_NAME);
                        Location spawn = store.getLocation(PATH);
                        if (spawn == null) {
                            spawn = player.getWorld().getSpawnLocation();
                        }
                        player.teleportAsync(spawn, TeleportCause.COMMAND);
                        Msg.ok(player, "Teleported to spawn.");
                    }))
                    .build(), "Teleport to the server spawn");

            reg.register(Commands.literal("setspawn")
                    .requires(s -> s.getSender().hasPermission("sessentials.setspawn"))
                    .executes(Cmds.playerExec(player -> {
                        YamlStore store = plugin.stores().get(STORE_NAME);
                        store.setLocation(PATH, player.getLocation());
                        store.save();
                        Msg.ok(player, "Spawn set to your current location.");
                    }))
                    .build(), "Set the server spawn to your current location");
        });
    }
}
