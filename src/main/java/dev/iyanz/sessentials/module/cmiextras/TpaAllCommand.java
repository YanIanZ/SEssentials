package dev.iyanz.sessentials.module.cmiextras;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.SmallCaps;
import dev.iyanz.sessentials.util.Style;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/**
 * {@code /tpaall} — invites <em>every</em> other online player to teleport to the
 * sender. Each invited player receives a clickable prompt and may run
 * {@code /tpaall accept} within {@value TpaAllInvites#EXPIRY_MILLIS} ms to be moved to
 * the requester with {@link Player#teleportAsync}.
 *
 * <p>Sending invites requires {@code sessentials.tpaall}; that check is done inside the
 * executor (not on the root node) so the ungated {@code accept} sub-literal stays
 * reachable for every invited player. Accepting requires no permission — it is a no-op
 * unless a live invite exists for the accepting player.</p>
 *
 * <p>Folia: invited players are messaged on their own region threads; on acceptance the
 * requester's location is read on the <em>requester's</em> region thread and the
 * accepter is moved with the thread-safe {@code teleportAsync}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class TpaAllCommand {

    private TpaAllCommand() {
    }

    /**
     * Registers {@code /tpaall} and its {@code accept} sub-literal.
     *
     * @param plugin  the owning plugin
     * @param reg     the Paper command registrar
     * @param invites the shared pending-invite registry
     */
    static void register(SEssentialsPlugin plugin, Commands reg, TpaAllInvites invites) {
        reg.register(Commands.literal("tpaall")
                .executes(Cmds.playerExec(requester -> inviteAll(plugin, invites, requester)))
                .then(Commands.literal("accept")
                        .executes(Cmds.playerExec(target -> accept(plugin, invites, target))))
                .build(), "Invite every online player to teleport to you");
    }

    /** Sends an invite (with a clickable accept prompt) to every other online player. */
    private static void inviteAll(SEssentialsPlugin plugin, TpaAllInvites invites, Player requester) {
        if (!requester.hasPermission("sessentials.tpaall")) {
            Msg.err(requester, "You do not have permission to do that.");
            return;
        }
        int invited = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(requester)) {
                continue;
            }
            invites.invite(target.getUniqueId(), requester.getUniqueId());
            String requesterName = requester.getName();
            Schedulers.entity(plugin, target, () -> {
                Msg.info(target, requesterName + " invites everyone to teleport to them (expires in 60s).");
                Msg.raw(target, "<click:run_command:'/tpaall accept'><hover:show_text:'Teleport to "
                        + requesterName + "'>" + Style.HINT + SmallCaps.of("[Click to accept]")
                        + "</hover></click>" + Style.GRAY + " " + SmallCaps.of("or run /tpaall accept"));
            });
            invited++;
        }
        if (invited == 0) {
            Msg.err(requester, "No one else is online to invite.");
        } else {
            Msg.ok(requester, "Invited " + invited + " player(s) to teleport to you.");
        }
    }

    /** Consumes the accepter's pending invite and moves them to the requester. */
    private static void accept(SEssentialsPlugin plugin, TpaAllInvites invites, Player target) {
        TpaAllInvites.Invite invite = invites.take(target.getUniqueId());
        if (invite == null) {
            Msg.err(target, "You have no pending teleport invite.");
            return;
        }
        Player requester = Bukkit.getPlayer(invite.requester());
        if (requester == null) {
            Msg.err(target, "That player is no longer online.");
            return;
        }
        Msg.ok(target, "Teleporting to " + requester.getName() + ".");
        Schedulers.entity(plugin, requester, () -> {
            Location destination = requester.getLocation().clone();
            target.teleportAsync(destination, TeleportCause.COMMAND);
            Msg.ok(requester, target.getName() + " accepted your teleport invite.");
        });
    }
}
