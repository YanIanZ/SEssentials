package dev.iyanz.sessentials.module.info;

import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Registers {@code /rules}: shows the server rules from {@code config.yml}'s
 * {@code info.rules} string list, or a sensible default 3-line list when unset.
 */
@SuppressWarnings("UnstableApiUsage")
final class RulesCommand {

    private static final String CONFIG_PATH = "info.rules";

    private static final List<String> DEFAULT_RULES = List.of(
            "<#9AA0A6>1. <#F5F5F5>Be respectful — no harassment, hate speech, or spam.",
            "<#9AA0A6>2. <#F5F5F5>No cheating, griefing, or exploiting bugs.",
            "<#9AA0A6>3. <#F5F5F5>Follow staff instructions at all times.");

    private RulesCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            var node = Commands.literal("rules")
                    .requires(s -> s.getSender().hasPermission("sessentials.rules"))
                    .executes(ctx -> {
                        InfoMessages.send(ctx.getSource().getSender(), plugin, CONFIG_PATH, DEFAULT_RULES);
                        return 1;
                    })
                    .build();
            reg.register(node, "Show the server rules");
        });
    }
}
