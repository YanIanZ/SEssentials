package dev.iyanz.sessentials.module.sendspawn;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/**
 * Send-to-spawn module: {@code /sendspawn <player>} teleports an online player to the
 * server spawn as an admin action (the target does not need any permission).
 *
 * <p>Behaviour:</p>
 * <ul>
 *   <li>{@code /sendspawn <player>} — sends the named online player to spawn
 *       (requires {@code sessentials.sendspawn}).</li>
 * </ul>
 *
 * <p>The destination mirrors the self-service {@code /spawn} command: the location
 * persisted by {@code /setspawn} in the {@code spawn} data store is preferred, falling
 * back to the target's current world's vanilla spawn when none has been set. The
 * teleport is entity work, so it is scheduled on the target's Folia region thread via
 * {@link Schedulers#entity} and performed with
 * {@link Player#teleportAsync(Location, TeleportCause)}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class SendSpawnModule implements EssModule {

    /** Name of the shared data store that holds the persisted server spawn. */
    private static final String STORE_NAME = "spawn";
    /** Path of the persisted spawn location inside the store. */
    private static final String PATH = "spawn";

    @Override
    public String name() {
        return "sendspawn";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("sendspawn")
                .requires(s -> s.getSender().hasPermission("sessentials.sendspawn"))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(Cmds.PLAYERS)
                        .executes(ctx -> send(plugin, ctx)))
                .build(), "Teleport a player to the server spawn"));
    }

    /**
     * Resolves the {@code <player>} argument and teleports that target to spawn from
     * the target's region thread.
     *
     * @param plugin the owning plugin
     * @param ctx    the command context holding the target name
     * @return 1 if a target was found and scheduled, 0 if the target was offline
     */
    private static int send(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String targetName = StringArgumentType.getString(ctx, "player");

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            Msg.err(sender, targetName + " is not online.");
            return 0;
        }
        boolean self = sender.equals(target);
        Schedulers.entity(plugin, target, () -> {
            Location spawn = plugin.stores().get(STORE_NAME).getLocation(PATH);
            if (spawn == null) {
                spawn = target.getWorld().getSpawnLocation();
            }
            target.teleportAsync(spawn, TeleportCause.COMMAND);
            Msg.info(target, "You were sent to spawn.");
            if (!self) {
                Msg.ok(sender, "Sent " + target.getName() + " to spawn.");
            }
        });
        return 1;
    }
}
