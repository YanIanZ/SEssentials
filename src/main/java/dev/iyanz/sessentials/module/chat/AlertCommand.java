package dev.iyanz.sessentials.module.chat;

import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.SmallCaps;
import dev.iyanz.sessentials.util.Style;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Registers {@code /alert} (alias {@code /broadcastbar}): a server-wide announcement
 * delivered two ways at once — a chat line (console + every player) and a brief
 * action bar — so it is hard to miss even mid-combat. The text is operator-supplied
 * and parsed as MiniMessage.
 */
@SuppressWarnings("UnstableApiUsage")
public final class AlertCommand {

    private AlertCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    public static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            var node = Commands.literal("alert")
                    .requires(s -> s.getSender().hasPermission("sessentials.alert"))
                    .then(Commands.argument("text", StringArgumentType.greedyString())
                            .executes(ctx -> send(plugin, ctx)))
                    .build();
            reg.register(node, "Server-wide alert (chat + action bar)", List.of("broadcastbar"));
        });
    }

    private static int send(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        String text = StringArgumentType.getString(ctx, "text");
        Component prefix = Msg.mm(Style.GRAY + "[" + Style.DANGER + SmallCaps.of("Alert") + Style.GRAY + "] ");
        Component chatLine = prefix.append(Msg.mm(text));
        Component actionBar = Msg.mm(text);

        Bukkit.getConsoleSender().sendMessage(chatLine);
        for (Player player : Bukkit.getOnlinePlayers()) {
            Schedulers.entity(plugin, player, () -> {
                player.sendMessage(chatLine);
                player.sendActionBar(actionBar);
            });
        }

        Msg.ok(ctx.getSource().getSender(), "Alert sent.");
        return 1;
    }
}
