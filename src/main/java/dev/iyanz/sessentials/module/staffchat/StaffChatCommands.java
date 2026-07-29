package dev.iyanz.sessentials.module.staffchat;

import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Registers and handles the staff-chat command: {@code /staffchat} (alias {@code
 * /schat}).
 *
 * <p>With no arguments the command toggles the calling player's staff-chat mode; with a
 * trailing message it sends that message straight to the staff channel without changing
 * the toggle. The {@code /sc} alias is intentionally avoided because the silent-chest
 * module already claims it.</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class StaffChatCommands {

    private final StaffChatService service;
    private SEssentialsPlugin plugin;

    /**
     * @param service the shared staff-chat state and delivery service
     */
    StaffChatCommands(StaffChatService service) {
        this.service = service;
    }

    /** Registers {@code /staffchat} and its {@code /schat} alias. */
    void register(SEssentialsPlugin plugin) {
        this.plugin = plugin;
        plugin.commands(reg -> reg.register(
                Commands.literal("staffchat")
                        .requires(s -> s.getSender().hasPermission(StaffChatService.PERMISSION))
                        .executes(Cmds.playerExec(this::toggle))
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(this::send))
                        .build(),
                "Toggle staff-chat mode, or send a message to the staff channel",
                List.of("schat")));
    }

    /** Toggles staff-chat mode for {@code player} and confirms the new state in Small Caps. */
    private void toggle(Player player) {
        if (service.toggle(player.getUniqueId())) {
            Msg.ok(player, "Staff chat mode enabled. Your messages now go to the staff channel.");
        } else {
            Msg.ok(player, "Staff chat mode disabled.");
        }
    }

    /** Sends the trailing message straight to the staff channel, attributed to the sender. */
    private int send(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String message = StringArgumentType.getString(ctx, "message");
        service.broadcast(plugin, sender.getName(), message);
        return 1;
    }
}
