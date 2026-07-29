package dev.iyanz.sessentials.module.info;

import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.command.CommandSender;

/**
 * Shared rendering for the config-driven line lists ({@code info.rules},
 * {@code info.motd}) used by both the {@code /rules}/{@code /motd} commands and the
 * join-time MOTD broadcast. Lines are operator-authored content, so each is rendered
 * via {@link Msg#mm(String)} (raw MiniMessage) rather than the plain-text Small Caps
 * helpers.
 */
final class InfoMessages {

    private InfoMessages() {
    }

    /**
     * Sends the configured lines at {@code configPath} to {@code to}, falling back to
     * {@code defaults} when the list is unset or empty.
     *
     * @param to         the recipient
     * @param plugin     the owning plugin (for config access)
     * @param configPath the {@code config.yml} string-list path
     * @param defaults   the lines to use when the config path is empty
     */
    static void send(CommandSender to, SEssentialsPlugin plugin, String configPath, List<String> defaults) {
        List<String> configured = plugin.getConfig().getStringList(configPath);
        List<String> lines = configured.isEmpty() ? defaults : configured;
        for (String line : lines) {
            to.sendMessage(Msg.mm(line));
        }
    }
}
