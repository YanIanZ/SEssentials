package dev.iyanz.sessentials.module.teleport;

import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/**
 * Registers {@code /back} (alias {@code /return}), which teleports the sender to the
 * location recorded in {@link TeleportHistory}.
 */
@SuppressWarnings("UnstableApiUsage")
final class BackCommand {

    private BackCommand() {
    }

    /**
     * Registers the {@code /back} command.
     *
     * @param plugin  the owning plugin
     * @param history the shared teleport-history store
     */
    static void register(SEssentialsPlugin plugin, TeleportHistory history) {
        plugin.commands(reg -> reg.register(
                Commands.literal("back")
                        .requires(s -> s.getSender().hasPermission("sessentials.back"))
                        .executes(Cmds.playerExec(player -> {
                            Location location = history.get(player.getUniqueId());
                            if (location == null) {
                                Msg.err(player, "You have no previous location to return to.");
                                return;
                            }
                            player.teleportAsync(location, TeleportCause.COMMAND);
                            Msg.ok(player, "Teleported to your previous location.");
                        }))
                        .build(),
                "Return to your previous location",
                List.of("return")));
    }
}
