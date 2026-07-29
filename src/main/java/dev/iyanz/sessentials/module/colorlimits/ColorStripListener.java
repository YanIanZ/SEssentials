package dev.iyanz.sessentials.module.colorlimits;

import dev.iyanz.sessentials.SEssentialsPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Flattens a chat message down to plain text — dropping any colour, gradient or other
 * formatting — for any sender without {@code sessentials.chat.color}.
 *
 * <p>Reads {@code color-limits.enabled} (default {@code false}) fresh on every message,
 * so a config reload takes effect immediately. A sender who holds the permission is
 * skipped entirely: this listener never touches their message, so they keep whatever
 * formatting they typed, or that another module (e.g. the {@code chat} module's
 * persisted {@code /chatcolor}) already applied.</p>
 *
 * <p>Runs at {@link EventPriority#HIGH} with {@code ignoreCancelled = true}. This is a
 * deliberate deviation from a same-priority-as-{@code chatfilter} ({@code LOW})
 * placement: the {@code chat} module's colour listener ({@link
 * dev.iyanz.sessentials.module.chat.ChatListener}) runs at {@code NORMAL} and rewrites
 * {@link AsyncChatEvent#message()} to the sender's persisted chat colour regardless of
 * this permission, so this listener must run strictly after it ({@code HIGH}) to be the
 * final word on formatting for a non-permitted sender. Running at {@code LOW} would let
 * that {@code NORMAL} recolour re-introduce formatting after the strip, defeating the
 * gate. {@code HIGH} is otherwise safe to share with {@code chatformat}'s listener,
 * which only replaces the renderer, never the message.</p>
 */
public final class ColorStripListener implements Listener {

    private final SEssentialsPlugin plugin;

    /**
     * @param plugin the owning plugin, used for its live {@code color-limits.*} config
     */
    public ColorStripListener(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Replaces the message with its plain text if the sender lacks the colour permission. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("color-limits.enabled", false)) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission("sessentials.chat.color")) {
            return; // keeps whatever formatting the player typed or another module applied
        }
        String plain = PlainTextComponentSerializer.plainText().serialize(event.message());
        event.message(Component.text(plain));
    }
}
