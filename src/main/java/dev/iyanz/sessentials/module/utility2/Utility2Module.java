package dev.iyanz.sessentials.module.utility2;

import java.util.List;

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
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/**
 * A second batch of general utility commands: a discard-on-close trash bin
 * ({@code /dispose}), ender chest clearing ({@code /clearender}), random
 * teleportation ({@code /rtp}), quick vertical/look-target hops ({@code /up},
 * {@code /down}, {@code /jump}) and a facing/spawn-bearing readout
 * ({@code /compass}).
 *
 * <p>Folia-safe: {@code /up}, {@code /down} and {@code /jump} read blocks at the
 * sender's own position (or along their line of sight), which is always safe from a
 * normal command executor since the sender is already on their own region thread.
 * {@code /rtp} picks a point that is very likely outside the sender's loaded region,
 * so its block reads are hopped onto that point's own region thread instead — see
 * {@link RandomTeleport}. Acting on another player ({@code /clearender <player>})
 * hops onto that target's region thread via {@link Schedulers#entity}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class Utility2Module implements EssModule {

    /** Default {@code /rtp} search radius (blocks), used when {@code rtp.radius} is unset. */
    private static final int DEFAULT_RTP_RADIUS = 5000;

    /** How far {@code /jump} looks for a block to teleport onto. */
    private static final int JUMP_RANGE = 64;

    private SEssentialsPlugin plugin;

    @Override
    public String name() {
        return "utility2";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(new DisposeListener(), plugin);

        plugin.commands(reg -> {
            reg.register(Commands.literal("dispose")
                    .requires(s -> s.getSender().hasPermission("sessentials.dispose"))
                    .executes(Cmds.playerExec(this::openTrash))
                    .build(), "Open a trash bin", List.of("trash"));

            reg.register(Commands.literal("clearender")
                    .requires(s -> s.getSender().hasPermission("sessentials.clearender"))
                    .executes(Cmds.playerExec(this::clearOwnEnderChest))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .requires(s -> s.getSender().hasPermission("sessentials.clearender.others"))
                            .executes(this::clearOthersEnderChest))
                    .build(), "Clear an ender chest", List.of("clearec"));

            reg.register(Commands.literal("rtp")
                    .requires(s -> s.getSender().hasPermission("sessentials.rtp"))
                    .executes(Cmds.playerExec(this::randomTeleport))
                    .build(), "Random teleport", List.of("wild", "randomtp"));

            reg.register(Commands.literal("up")
                    .requires(s -> s.getSender().hasPermission("sessentials.up"))
                    .executes(Cmds.playerExec(this::teleportUp))
                    .build(), "Teleport up to the next solid block");

            reg.register(Commands.literal("down")
                    .requires(s -> s.getSender().hasPermission("sessentials.down"))
                    .executes(Cmds.playerExec(this::teleportDown))
                    .build(), "Teleport down to solid ground");

            reg.register(Commands.literal("jump")
                    .requires(s -> s.getSender().hasPermission("sessentials.jump"))
                    .executes(Cmds.playerExec(this::teleportToTarget))
                    .build(), "Teleport to the block you're looking at", List.of("thru"));

            reg.register(Commands.literal("compass")
                    .requires(s -> s.getSender().hasPermission("sessentials.compass"))
                    .executes(Cmds.playerExec(this::compass))
                    .build(), "Show your facing and the direction to spawn");
        });
    }

    // --- dispose -------------------------------------------------------------

    private void openTrash(Player player) {
        player.openInventory(DisposeMenu.create());
        Msg.info(player, "Anything left here when you close it is gone for good.");
    }

    // --- clearender ------------------------------------------------------------

    private void clearOwnEnderChest(Player player) {
        player.getEnderChest().clear();
        Msg.ok(player, "Your ender chest has been cleared.");
    }

    private int clearOthersEnderChest(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Msg.err(sender, name + " is not online.");
            return 0;
        }
        Schedulers.entity(plugin, target, () -> {
            target.getEnderChest().clear();
            Msg.info(target, "Your ender chest was cleared by an operator.");
        });
        Msg.ok(sender, "Cleared " + target.getName() + "'s ender chest.");
        return 1;
    }

    // --- rtp -------------------------------------------------------------------

    private void randomTeleport(Player player) {
        int radius = Math.max(1, plugin.getConfig().getInt("rtp.radius", DEFAULT_RTP_RADIUS));
        RandomTeleport.attempt(plugin, player, radius);
    }

    // --- up / down / jump --------------------------------------------------

    private void teleportUp(Player player) {
        Location current = player.getLocation();
        World world = player.getWorld();
        int x = current.getBlockX();
        int z = current.getBlockZ();
        for (int y = current.getBlockY() + 1; y < world.getMaxHeight(); y++) {
            Block block = world.getBlockAt(x, y, z);
            if (SafeGround.isSafe(block)) {
                Location destination = new Location(world, x + 0.5, y + 1, z + 0.5,
                        current.getYaw(), current.getPitch());
                player.teleportAsync(destination, TeleportCause.COMMAND);
                Msg.ok(player, "Teleported up.");
                return;
            }
        }
        Msg.err(player, "No solid block found above you.");
    }

    private void teleportDown(Player player) {
        Location current = player.getLocation();
        World world = player.getWorld();
        int x = current.getBlockX();
        int z = current.getBlockZ();
        for (int y = current.getBlockY() - 1; y >= world.getMinHeight(); y--) {
            Block block = world.getBlockAt(x, y, z);
            if (SafeGround.isSafe(block)) {
                Location destination = new Location(world, x + 0.5, y + 1, z + 0.5,
                        current.getYaw(), current.getPitch());
                player.teleportAsync(destination, TeleportCause.COMMAND);
                Msg.ok(player, "Teleported down.");
                return;
            }
        }
        Msg.err(player, "No solid ground found below you.");
    }

    private void teleportToTarget(Player player) {
        Block target = player.getTargetBlockExact(JUMP_RANGE);
        if (target == null) {
            Msg.err(player, "No block in sight within " + JUMP_RANGE + " blocks.");
            return;
        }
        Location current = player.getLocation();
        Location destination = new Location(target.getWorld(), target.getX() + 0.5, target.getY() + 1,
                target.getZ() + 0.5, current.getYaw(), current.getPitch());
        player.teleportAsync(destination, TeleportCause.COMMAND);
        Msg.ok(player, "Teleported to the targeted block.");
    }

    // --- compass -------------------------------------------------------------

    private void compass(Player player) {
        Location current = player.getLocation();
        Msg.value(player, "Facing:", Bearing.cardinal(current.getYaw()));

        Location spawn = player.getWorld().getSpawnLocation();
        double dx = spawn.getX() - current.getX();
        double dz = spawn.getZ() - current.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 1.0) {
            Msg.value(player, "Spawn:", "you are here");
        } else {
            String direction = Bearing.cardinal(Bearing.yawTo(dx, dz));
            Msg.value(player, "Spawn:", direction + " (" + Math.round(distance) + "m)");
        }
    }
}
