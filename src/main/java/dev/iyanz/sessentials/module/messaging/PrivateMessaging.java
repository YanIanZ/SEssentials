package dev.iyanz.sessentials.module.messaging;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.SmallCaps;
import dev.iyanz.sessentials.util.Style;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Registers and handles the private-messaging commands: {@code /msg} (and its
 * aliases), {@code /reply} and {@code /socialspy}.
 *
 * <p>Player-supplied message text is never parsed as MiniMessage — it is wrapped
 * with {@link Component#text(String)} so a player cannot inject colour tags, click
 * events or similar markup into another player's chat. Only the surrounding label
 * (sender/target names, brackets, arrow) is coloured.</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class PrivateMessaging {

    private static final String ME = SmallCaps.of("Me");

    private final Map<UUID, UUID> lastMessaged;
    private final Set<UUID> spies;
    private SEssentialsPlugin plugin;

    /**
     * @param lastMessaged shared map of player &rarr; last player they exchanged a
     *                      private message with, updated on both send and receive
     * @param spies        shared set of players currently watching private messages
     */
    PrivateMessaging(Map<UUID, UUID> lastMessaged, Set<UUID> spies) {
        this.lastMessaged = lastMessaged;
        this.spies = spies;
    }

    /** Registers {@code /msg}, {@code /reply} and {@code /socialspy}. */
    void register(SEssentialsPlugin plugin) {
        this.plugin = plugin;
        plugin.commands(reg -> {
            reg.register(Commands.literal("msg")
                    .requires(s -> s.getSender().hasPermission("sessentials.msg"))
                    .then(Commands.argument("target", StringArgumentType.word())
                            .suggests(Cmds.PLAYERS)
                            .then(Commands.argument("message", StringArgumentType.greedyString())
                                    .executes(this::handleMsg)))
                    .build(), "Send a private message", List.of("tell", "w", "pm", "whisper"));

            reg.register(Commands.literal("reply")
                    .requires(s -> s.getSender().hasPermission("sessentials.reply"))
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                            .executes(this::handleReply))
                    .build(), "Reply to your last private message", List.of("r"));

            reg.register(Commands.literal("socialspy")
                    .requires(s -> s.getSender().hasPermission("sessentials.socialspy"))
                    .executes(Cmds.playerExec(this::toggleSpy))
                    .build(), "Toggle seeing others' private messages", List.of("ss"));
        });
    }

    private int handleMsg(CommandContext<CommandSourceStack> ctx) {
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
        if (target.getUniqueId().equals(sender.getUniqueId())) {
            Msg.err(sender, "You can't message yourself.");
            return 0;
        }
        String message = StringArgumentType.getString(ctx, "message");
        deliver(sender, target, message);
        return 1;
    }

    private int handleReply(CommandContext<CommandSourceStack> ctx) {
        Player sender = Cmds.player(ctx);
        if (sender == null) {
            return 0;
        }
        UUID lastId = lastMessaged.get(sender.getUniqueId());
        Player target = lastId == null ? null : Bukkit.getPlayer(lastId);
        if (target == null) {
            Msg.err(sender, "You have no one to reply to.");
            return 0;
        }
        String message = StringArgumentType.getString(ctx, "message");
        deliver(sender, target, message);
        return 1;
    }

    private void toggleSpy(Player player) {
        if (spies.remove(player.getUniqueId())) {
            Msg.ok(player, "Social spy disabled.");
        } else {
            spies.add(player.getUniqueId());
            Msg.ok(player, "Social spy enabled.");
        }
    }

    /**
     * Delivers a private message: sends the sender- and target-formatted copies,
     * records both directions of the reply-tracking map, and mirrors a spy-formatted
     * copy to any online social-spy watchers other than the sender/target.
     * The target (and any spies) are messaged on their own region thread; the
     * command sender is messaged directly since a normal command executor already
     * runs on the sender's region thread.
     */
    private void deliver(Player sender, Player target, String text) {
        lastMessaged.put(sender.getUniqueId(), target.getUniqueId());
        lastMessaged.put(target.getUniqueId(), sender.getUniqueId());

        Component toSender = label(ME, target.getName()).append(Component.text(text, NamedTextColor.WHITE));
        Component toTarget = label(sender.getName(), ME).append(Component.text(text, NamedTextColor.WHITE));
        sender.sendMessage(toSender);
        Schedulers.entity(plugin, target, () -> target.sendMessage(toTarget));

        if (spies.isEmpty()) {
            return;
        }
        Component spyLine = spyLabel(sender.getName(), target.getName()).append(Component.text(text, NamedTextColor.WHITE));
        UUID senderId = sender.getUniqueId();
        UUID targetId = target.getUniqueId();
        for (Player online : Bukkit.getOnlinePlayers()) {
            UUID id = online.getUniqueId();
            if (id.equals(senderId) || id.equals(targetId) || !spies.contains(id)) {
                continue;
            }
            Schedulers.entity(plugin, online, () -> online.sendMessage(spyLine));
        }
    }

    /** {@code [left ➜ right] } label, small-caps green/grey/white palette. */
    private Component label(String left, String right) {
        return Msg.mm(Style.GRAY + "[" + Style.OK + left + " " + Style.GRAY + "➜ " + Style.VALUE + right + Style.GRAY + "] ");
    }

    /** {@code [spy from ➜ to] } label used for the social-spy mirrored copy. */
    private Component spyLabel(String from, String to) {
        return Msg.mm(Style.GRAY + "[" + Style.BAD + SmallCaps.of("Spy") + Style.GRAY + " " + Style.VALUE + from
                + Style.GRAY + " ➜ " + Style.VALUE + to + Style.GRAY + "] ");
    }
}
