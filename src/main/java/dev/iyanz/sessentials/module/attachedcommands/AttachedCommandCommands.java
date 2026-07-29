package dev.iyanz.sessentials.module.attachedcommands;

import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * {@code /attachcommand <command...>} (alias {@code /attachcmd}) and
 * {@code /detachcommand} (alias {@code /detachcmd}): tag the item in the sender's
 * hand so right-clicking it later runs an operator-chosen command as the holder
 * (see {@link AttachedCommandItems} and {@link AttachedCommandListener}).
 */
@SuppressWarnings("UnstableApiUsage")
final class AttachedCommandCommands {

    private static final String PERMISSION = "sessentials.attachcommand";

    private AttachedCommandCommands() {
    }

    /**
     * Registers both commands against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(
                Commands.literal("attachcommand")
                        .requires(s -> s.getSender().hasPermission(PERMISSION))
                        .then(Commands.argument("command", StringArgumentType.greedyString())
                                .executes(ctx -> attach(plugin, ctx)))
                        .build(),
                "Attach a command to the item in your hand",
                List.of("attachcmd")));

        plugin.commands(reg -> reg.register(
                Commands.literal("detachcommand")
                        .requires(s -> s.getSender().hasPermission(PERMISSION))
                        .executes(ctx -> detach(plugin, ctx))
                        .build(),
                "Detach the command from the item in your hand",
                List.of("detachcmd")));
    }

    private static int attach(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        ItemStack hand = heldItemOrError(player);
        if (hand == null) {
            return 0;
        }
        String command = StringArgumentType.getString(ctx, "command");
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        if (command.isBlank()) {
            Msg.err(player, "You must specify a command.");
            return 0;
        }
        AttachedCommandItems.attach(plugin, hand, command);
        Msg.ok(player, "Command attached to your held item.");
        return 1;
    }

    private static int detach(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        ItemStack hand = heldItemOrError(player);
        if (hand == null) {
            return 0;
        }
        if (AttachedCommandItems.detach(plugin, hand) == null) {
            Msg.err(player, "That item has no attached command.");
            return 0;
        }
        Msg.ok(player, "Command detached from your held item.");
        return 1;
    }

    /** @return the sender's held item, or {@code null} (after messaging) if empty-handed. */
    private static ItemStack heldItemOrError(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            Msg.err(player, "You are not holding an item.");
            return null;
        }
        return hand;
    }
}
