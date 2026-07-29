package dev.iyanz.sessentials.module.chat;

import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Registers {@code /clearchat} (alias {@code /cc}): pushes ~100 blank lines to every
 * online player's chat, "clearing" it. Each player's blank lines are sent on that
 * player's own region thread (Folia-safe).
 */
@SuppressWarnings("UnstableApiUsage")
public final class ClearChatCommand {

    private static final int BLANK_LINES = 100;

    private ClearChatCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    public static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            var node = Commands.literal("clearchat")
                    .requires(s -> s.getSender().hasPermission("sessentials.clearchat"))
                    .executes(ctx -> {
                        for (Player online : Bukkit.getOnlinePlayers()) {
                            Schedulers.entity(plugin, online, () -> {
                                for (int i = 0; i < BLANK_LINES; i++) {
                                    online.sendMessage(Component.empty());
                                }
                            });
                        }
                        Msg.ok(ctx.getSource().getSender(), "Chat cleared.");
                        return 1;
                    })
                    .build();
            reg.register(node, "Clear everyone's chat", List.of("cc"));
        });
    }
}
