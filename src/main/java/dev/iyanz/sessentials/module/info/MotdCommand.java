package dev.iyanz.sessentials.module.info;

import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Registers {@code /motd}: shows the message of the day from {@code config.yml}'s
 * {@code info.motd} string list, or a friendly default welcome when unset. The same
 * lines are broadcast to players automatically on join by {@link MotdListener}.
 */
@SuppressWarnings("UnstableApiUsage")
final class MotdCommand {

    static final String CONFIG_PATH = "info.motd";

    static final List<String> DEFAULT_MOTD = List.of(
            "<gradient:#43C6AC:#4FC3F7><bold>Welcome to SourbyCraft!</bold></gradient>",
            "<#9AA0A6>Type <#F5F5F5>/rules<#9AA0A6> to review the server rules.",
            "<#9AA0A6>We hope you enjoy your stay!");

    private MotdCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            var node = Commands.literal("motd")
                    .requires(s -> s.getSender().hasPermission("sessentials.motd"))
                    .executes(ctx -> {
                        InfoMessages.send(ctx.getSource().getSender(), plugin, CONFIG_PATH, DEFAULT_MOTD);
                        return 1;
                    })
                    .build();
            reg.register(node, "Show the message of the day");
        });
    }
}
