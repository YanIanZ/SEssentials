package dev.iyanz.sessentials.module.chatformat;

import dev.iyanz.sessentials.SEssentialsPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Rebuilds every chat message's rendering using an operator-configured MiniMessage
 * format, via {@link AsyncChatEvent#renderer}.
 *
 * <p>Reads {@code chat-format.enabled} (default {@code false} — disabled unless an
 * operator opts in, so this never fights another chat-formatting plugin) and
 * {@code chat-format.format} (default {@code "<gray>{player}<gray>: <white>{message}"})
 * fresh on every message, so a config reload takes effect immediately.</p>
 *
 * <p>The format string itself is operator-authored and trusted, but the two values it
 * interpolates are not: the sender's display name and chat message are always injected
 * as literal, already-built components via MiniMessage {@link Placeholder#component},
 * never flattened to text and re-parsed as MiniMessage. That means a player can never
 * use their message, or a nickname plugin's display name, to inject markup, colours, or
 * click/hover events into the rendered format.</p>
 *
 * <p>Runs at {@link EventPriority#HIGH} with {@code ignoreCancelled = true}: high enough
 * that it composes cleanly after SEssentials' own message-affecting listeners — the
 * moderation mute check at {@code LOWEST}, {@code chatfilter} at {@code LOW}, and the
 * chat-colour {@link dev.iyanz.sessentials.module.chat.ChatListener} at {@code NORMAL}
 * — have already had a chance to run, and it is skipped entirely once a message has
 * been cancelled upstream. Only the renderer is replaced here; the message content
 * produced by those listeners is left untouched and is exactly what is substituted for
 * {@code {message}}.</p>
 */
public final class ChatFormatListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    /** Used when {@code chat-format.format} is unset. */
    private static final String DEFAULT_FORMAT = "<gray>{player}<gray>: <white>{message}";

    private final SEssentialsPlugin plugin;

    /**
     * @param plugin the owning plugin, used for its live {@code chat-format.*} config
     */
    public ChatFormatListener(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Installs the configured chat renderer, but only if {@code chat-format.enabled} is true. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat-format.enabled", false)) {
            return; // feature opt-in only, so we never fight another chat plugin by default
        }
        String format = plugin.getConfig().getString("chat-format.format", DEFAULT_FORMAT);
        // Turn the operator's {player}/{message} placeholders into MiniMessage tags; the
        // template itself is trusted, only the values bound to those tags are player-supplied.
        String template = format.replace("{player}", "<player>").replace("{message}", "<message>");

        event.renderer((source, sourceDisplayName, message, viewer) -> MM.deserialize(template,
                Placeholder.component("player", sourceDisplayName),
                Placeholder.component("message", message)));
    }
}
