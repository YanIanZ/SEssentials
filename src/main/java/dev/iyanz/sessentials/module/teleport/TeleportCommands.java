package dev.iyanz.sessentials.module.teleport;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/**
 * Registers the direct-teleport commands: {@code /tp}, {@code /tphere}, {@code /tpall},
 * {@code /tppos} and {@code /top}.
 */
@SuppressWarnings("UnstableApiUsage")
final class TeleportCommands {

    private TeleportCommands() {
    }

    /**
     * Registers all direct-teleport commands.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            reg.register(buildTp(plugin), "Teleport to a player, or teleport a player to another player");
            reg.register(buildTpHere(plugin), "Bring a player to you");
            reg.register(buildTpAll(plugin), "Teleport every online player to you");
            reg.register(buildTpPos(), "Teleport to exact coordinates in your world");
            reg.register(buildTop(), "Teleport to the highest block at your position");
        });
    }

    private static com.mojang.brigadier.tree.LiteralCommandNode<io.papermc.paper.command.brigadier.CommandSourceStack> buildTp(SEssentialsPlugin plugin) {
        return Commands.literal("tp")
                .requires(s -> s.getSender().hasPermission("sessentials.tp"))
                .then(Commands.argument("target", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                        .executes(ctx -> {
                            // /tp <target> — teleport the sender to an online player.
                            Player sender = Cmds.player(ctx);
                            if (sender == null) {
                                return 0;
                            }
                            String name = StringArgumentType.getString(ctx, "target");
                            Player target = Bukkit.getPlayerExact(name);
                            if (target == null) {
                                Msg.err(sender, name + " is not online.");
                                return 0;
                            }
                            if (target.equals(sender)) {
                                Msg.err(sender, "You are already there.");
                                return 0;
                            }
                            Msg.ok(sender, "Teleporting to " + target.getName() + "...");
                            Teleports.moveToPlayer(plugin, sender, target);
                            return 1;
                        })
                        .then(Commands.argument("destination", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                                .requires(s -> s.getSender().hasPermission("sessentials.tp.others"))
                                .executes(ctx -> {
                                    // /tp <player> <target> — teleport <player> to <target>.
                                    String moverName = StringArgumentType.getString(ctx, "target");
                                    String destName = StringArgumentType.getString(ctx, "destination");
                                    var console = ctx.getSource().getSender();
                                    Player mover = Bukkit.getPlayerExact(moverName);
                                    if (mover == null) {
                                        Msg.err(console, moverName + " is not online.");
                                        return 0;
                                    }
                                    Player destination = Bukkit.getPlayerExact(destName);
                                    if (destination == null) {
                                        Msg.err(console, destName + " is not online.");
                                        return 0;
                                    }
                                    if (mover.equals(destination)) {
                                        Msg.err(console, moverName + " is already there.");
                                        return 0;
                                    }
                                    Msg.ok(console, "Teleporting " + mover.getName() + " to " + destination.getName() + "...");
                                    Teleports.moveToPlayer(plugin, mover, destination);
                                    return 1;
                                })))
                .build();
    }

    private static com.mojang.brigadier.tree.LiteralCommandNode<io.papermc.paper.command.brigadier.CommandSourceStack> buildTpHere(SEssentialsPlugin plugin) {
        return Commands.literal("tphere")
                .requires(s -> s.getSender().hasPermission("sessentials.tphere"))
                .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                        .executes(ctx -> {
                            Player sender = Cmds.player(ctx);
                            if (sender == null) {
                                return 0;
                            }
                            String name = StringArgumentType.getString(ctx, "player");
                            Player target = Bukkit.getPlayerExact(name);
                            if (target == null) {
                                Msg.err(sender, name + " is not online.");
                                return 0;
                            }
                            if (target.equals(sender)) {
                                Msg.err(sender, "You are already there.");
                                return 0;
                            }
                            Msg.ok(sender, "Bringing " + target.getName() + " to you...");
                            Teleports.moveToPlayer(plugin, target, sender);
                            Schedulers.entity(plugin, target, () ->
                                    Msg.info(target, "You have been teleported to " + sender.getName() + "."));
                            return 1;
                        }))
                .build();
    }

    private static com.mojang.brigadier.tree.LiteralCommandNode<io.papermc.paper.command.brigadier.CommandSourceStack> buildTpAll(SEssentialsPlugin plugin) {
        return Commands.literal("tpall")
                .requires(s -> s.getSender().hasPermission("sessentials.tpall"))
                .executes(Cmds.playerExec(sender -> {
                    int moved = 0;
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (online.equals(sender)) {
                            continue;
                        }
                        Teleports.moveToPlayer(plugin, online, sender);
                        Schedulers.entity(plugin, online, () ->
                                Msg.info(online, "You have been teleported to " + sender.getName() + "."));
                        moved++;
                    }
                    Msg.ok(sender, "Teleported " + moved + " player(s) to you.");
                }))
                .build();
    }

    private static com.mojang.brigadier.tree.LiteralCommandNode<io.papermc.paper.command.brigadier.CommandSourceStack> buildTpPos() {
        return Commands.literal("tppos")
                .requires(s -> s.getSender().hasPermission("sessentials.tppos"))
                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                        .executes(ctx -> {
                                            Player player = Cmds.player(ctx);
                                            if (player == null) {
                                                return 0;
                                            }
                                            double x = DoubleArgumentType.getDouble(ctx, "x");
                                            double y = DoubleArgumentType.getDouble(ctx, "y");
                                            double z = DoubleArgumentType.getDouble(ctx, "z");
                                            Location current = player.getLocation();
                                            Location destination = new Location(player.getWorld(), x, y, z,
                                                    current.getYaw(), current.getPitch());
                                            player.teleportAsync(destination, TeleportCause.COMMAND);
                                            Msg.ok(player, "Teleported to the given coordinates.");
                                            return 1;
                                        }))))
                .build();
    }

    private static com.mojang.brigadier.tree.LiteralCommandNode<io.papermc.paper.command.brigadier.CommandSourceStack> buildTop() {
        return Commands.literal("top")
                .requires(s -> s.getSender().hasPermission("sessentials.top"))
                .executes(Cmds.playerExec(player -> {
                    Location current = player.getLocation();
                    World world = player.getWorld();
                    int highestY = world.getHighestBlockAt(current.getBlockX(), current.getBlockZ()).getY();
                    Location destination = new Location(world, current.getX(), highestY + 1, current.getZ(),
                            current.getYaw(), current.getPitch());
                    player.teleportAsync(destination, TeleportCause.COMMAND);
                    Msg.ok(player, "Teleported to the highest block.");
                }))
                .build();
    }
}
