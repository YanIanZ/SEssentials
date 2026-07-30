package dev.iyanz.sessentials.module.cmiextras;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/**
 * {@code /tpo <player>} — admin teleport <em>override</em>: teleports the sender to the
 * target unconditionally. Unlike the consent-based {@code /tpa} flow this never asks and
 * never consults the target's teleport toggle, making it suitable for moderation.
 * Requires {@code sessentials.tpo}.
 *
 * <p>Folia: the target's {@link Location} is read on the <em>target's</em> region thread
 * (the only thread allowed to touch that entity), then the sender is moved with
 * {@link Player#teleportAsync}, which is safe to call from any thread.</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class TpoCommand {

    private TpoCommand() {
    }

    /**
     * Registers {@code /tpo <player>}.
     *
     * @param plugin the owning plugin
     * @param reg    the Paper command registrar
     */
    static void register(SEssentialsPlugin plugin, Commands reg) {
        reg.register(Commands.literal("tpo")
                .requires(s -> s.getSender().hasPermission("sessentials.tpo"))
                .then(Commands.argument("target", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                        .executes(ctx -> teleportOverride(plugin, ctx)))
                .build(), "Teleport to a player, bypassing their teleport toggle");
    }

    /** Moves the sending player straight to the named target. */
    private static int teleportOverride(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
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
            Msg.err(sender, "You are already at your own location.");
            return 0;
        }
        Msg.ok(sender, "Teleporting to " + target.getName() + ".");
        Schedulers.entity(plugin, target, () -> {
            Location destination = target.getLocation().clone();
            sender.teleportAsync(destination, TeleportCause.COMMAND);
        });
        return 1;
    }
}
