package dev.iyanz.sessentials.module.teleport;

import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Registers the consent-based teleport-request commands: {@code /tpa}, {@code /tpahere},
 * {@code /tpaccept} (alias {@code /tpyes}) and {@code /tpdeny} (alias {@code /tpno}).
 * Pending requests are tracked in {@link TeleportRequests}.
 */
@SuppressWarnings("UnstableApiUsage")
final class TeleportRequestCommands {

    private TeleportRequestCommands() {
    }

    /**
     * Registers all teleport-request commands.
     *
     * @param plugin   the owning plugin
     * @param requests the shared pending-request registry
     */
    static void register(SEssentialsPlugin plugin, TeleportRequests requests) {
        plugin.commands(reg -> {
            reg.register(buildTpa(plugin, requests), "Request to teleport to a player");
            reg.register(buildTpaHere(plugin, requests), "Request that a player teleport to you");
            reg.register(buildTpAccept(plugin, requests), "Accept the pending teleport request", List.of("tpyes"));
            reg.register(buildTpDeny(plugin, requests), "Deny the pending teleport request", List.of("tpno"));
        });
    }

    private static com.mojang.brigadier.tree.LiteralCommandNode<io.papermc.paper.command.brigadier.CommandSourceStack> buildTpa(
            SEssentialsPlugin plugin, TeleportRequests requests) {
        return Commands.literal("tpa")
                .requires(s -> s.getSender().hasPermission("sessentials.tpa"))
                .then(Commands.argument("target", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                        .executes(ctx -> {
                            Player requester = Cmds.player(ctx);
                            if (requester == null) {
                                return 0;
                            }
                            String name = StringArgumentType.getString(ctx, "target");
                            Player target = Bukkit.getPlayerExact(name);
                            if (target == null) {
                                Msg.err(requester, name + " is not online.");
                                return 0;
                            }
                            if (target.equals(requester)) {
                                Msg.err(requester, "You cannot request to teleport to yourself.");
                                return 0;
                            }
                            requests.request(target.getUniqueId(), requester.getUniqueId(), TeleportRequests.Kind.TO_TARGET);
                            Msg.ok(requester, "Teleport request sent to " + target.getName() + ".");
                            Schedulers.entity(plugin, target, () -> Msg.info(target, requester.getName()
                                    + " has requested to teleport to you. Use /tpaccept or /tpdeny (expires in 60s)."));
                            return 1;
                        }))
                .build();
    }

    private static com.mojang.brigadier.tree.LiteralCommandNode<io.papermc.paper.command.brigadier.CommandSourceStack> buildTpaHere(
            SEssentialsPlugin plugin, TeleportRequests requests) {
        return Commands.literal("tpahere")
                .requires(s -> s.getSender().hasPermission("sessentials.tpahere"))
                .then(Commands.argument("target", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                        .executes(ctx -> {
                            Player requester = Cmds.player(ctx);
                            if (requester == null) {
                                return 0;
                            }
                            String name = StringArgumentType.getString(ctx, "target");
                            Player target = Bukkit.getPlayerExact(name);
                            if (target == null) {
                                Msg.err(requester, name + " is not online.");
                                return 0;
                            }
                            if (target.equals(requester)) {
                                Msg.err(requester, "You cannot request that you teleport to yourself.");
                                return 0;
                            }
                            requests.request(target.getUniqueId(), requester.getUniqueId(), TeleportRequests.Kind.TARGET_TO_REQUESTER);
                            Msg.ok(requester, "Teleport request sent to " + target.getName() + ".");
                            Schedulers.entity(plugin, target, () -> Msg.info(target, requester.getName()
                                    + " has requested that you teleport to them. Use /tpaccept or /tpdeny (expires in 60s)."));
                            return 1;
                        }))
                .build();
    }

    private static com.mojang.brigadier.tree.LiteralCommandNode<io.papermc.paper.command.brigadier.CommandSourceStack> buildTpAccept(
            SEssentialsPlugin plugin, TeleportRequests requests) {
        return Commands.literal("tpaccept")
                .requires(s -> s.getSender().hasPermission("sessentials.tpaccept"))
                .executes(Cmds.playerExec(target -> {
                    TeleportRequests.Request req = requests.take(target.getUniqueId());
                    if (req == null) {
                        Msg.err(target, "You have no pending teleport request.");
                        return;
                    }
                    Player requester = Bukkit.getPlayer(req.requester());
                    if (requester == null) {
                        Msg.err(target, "That player is no longer online.");
                        return;
                    }
                    Msg.ok(target, "Accepted " + requester.getName() + "'s teleport request.");
                    switch (req.kind()) {
                        case TO_TARGET -> Teleports.moveToPlayer(plugin, requester, target);
                        case TARGET_TO_REQUESTER -> Teleports.moveToPlayer(plugin, target, requester);
                    }
                    Schedulers.entity(plugin, requester, () ->
                            Msg.ok(requester, target.getName() + " accepted your teleport request."));
                }))
                .build();
    }

    private static com.mojang.brigadier.tree.LiteralCommandNode<io.papermc.paper.command.brigadier.CommandSourceStack> buildTpDeny(
            SEssentialsPlugin plugin, TeleportRequests requests) {
        return Commands.literal("tpdeny")
                .requires(s -> s.getSender().hasPermission("sessentials.tpdeny"))
                .executes(Cmds.playerExec(target -> {
                    TeleportRequests.Request req = requests.take(target.getUniqueId());
                    if (req == null) {
                        Msg.err(target, "You have no pending teleport request.");
                        return;
                    }
                    Msg.info(target, "Teleport request denied.");
                    Player requester = Bukkit.getPlayer(req.requester());
                    if (requester != null) {
                        Schedulers.entity(plugin, requester, () ->
                                Msg.err(requester, target.getName() + " denied your teleport request."));
                    }
                }))
                .build();
    }
}
