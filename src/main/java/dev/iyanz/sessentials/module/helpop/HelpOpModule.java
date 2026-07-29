package dev.iyanz.sessentials.module.helpop;

import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Player-to-staff help requests: {@code /helpop <message...>} (alias {@code /request})
 * lets any permitted player flag down online staff without knowing who is on duty.
 *
 * <p>The request is delivered to the console and to every online player holding
 * {@link #RECEIVE_PERMISSION} (staff), formatted as a coloured {@code [HelpOp]} line
 * that names the sender. The sender always receives a small-caps confirmation; if no
 * staff happen to be online, the confirmation is followed by a note so the sender
 * knows their message may go unseen for a while.</p>
 *
 * <p>The message body is wrapped with {@link Component#text(String)} rather than parsed
 * as MiniMessage, so a player can never inject colour tags, click events or other markup
 * into staff chat &mdash; only the surrounding label is coloured.</p>
 *
 * <p>Folia-safe: the command executor runs on the sender's own region thread, and each
 * staff recipient is messaged on their own region thread via
 * {@link Schedulers#entity(org.bukkit.plugin.Plugin, org.bukkit.entity.Entity, Runnable)}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class HelpOpModule implements EssModule {

    /** Permission required to send a help request. */
    static final String SEND_PERMISSION = "sessentials.helpop";

    /** Permission an online player must hold to receive help requests. */
    static final String RECEIVE_PERMISSION = "sessentials.helpop.receive";

    @Override
    public String name() {
        return "helpop";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("helpop")
                .requires(s -> s.getSender().hasPermission(SEND_PERMISSION))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> handleHelpOp(plugin, ctx)))
                .build(), "Send a help request to online staff", List.of("request")));
    }

    private static int handleHelpOp(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String text = StringArgumentType.getString(ctx, "message");
        Component message = Msg.mm("<#FFD54F>[HelpOp] <#F5F5F5>" + sender.getName() + "<#9AA0A6>: ")
                .append(Component.text(text, NamedTextColor.WHITE));

        boolean anyStaffOnline = broadcast(plugin, message);

        Msg.ok(sender, "Your message was sent to staff.");
        if (!anyStaffOnline) {
            Msg.info(sender, "No staff are currently online.");
        }
        return 1;
    }

    /**
     * Sends {@code message} to the console and to every online holder of
     * {@link #RECEIVE_PERMISSION}, each on its own region thread (Folia-safe).
     *
     * @param plugin  the owning plugin, used for the region-thread hop
     * @param message the pre-formatted request line to deliver
     * @return {@code true} if at least one online staff member received the request
     */
    private static boolean broadcast(SEssentialsPlugin plugin, Component message) {
        Bukkit.getConsoleSender().sendMessage(message);
        boolean anyStaffOnline = false;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission(RECEIVE_PERMISSION)) {
                continue;
            }
            anyStaffOnline = true;
            Schedulers.entity(plugin, player, () -> player.sendMessage(message));
        }
        return anyStaffOnline;
    }
}
