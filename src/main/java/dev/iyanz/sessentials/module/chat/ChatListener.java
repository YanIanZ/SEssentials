package dev.iyanz.sessentials.module.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Re-colours a player's chat message using their persisted {@code /chatcolor} choice.
 *
 * <p>The player's message is always re-derived as PLAIN text via {@link
 * PlainTextComponentSerializer} and injected as a literal {@link Component} through a
 * MiniMessage placeholder — <strong>never</strong> re-parsed as MiniMessage — so a
 * player can never use their own chat text to inject markup, colours or click/hover
 * events. Only the operator-validated tag from {@link ChatColorService} is ever
 * deserialized.</p>
 *
 * <p>Runs at {@link EventPriority#NORMAL} with {@code ignoreCancelled = true}, so it
 * naturally no-ops for messages a lower-priority listener (e.g. a mute check) has
 * already cancelled.</p>
 */
public final class ChatListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final ChatColorService colorService;

    /**
     * @param colorService the service holding each player's persisted chat colour
     */
    public ChatListener(ChatColorService colorService) {
        this.colorService = colorService;
    }

    /** Rebuilds the chat message with the sender's persisted colour/gradient, if any. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String tag = colorService.colorOf(player.getUniqueId());
        if (tag == null) {
            return; // no persisted colour; leave the default message/renderer untouched
        }
        String plain = PlainTextComponentSerializer.plainText().serialize(event.message());
        Component recolored = MM.deserialize(tag + "<msg>", Placeholder.component("msg", Component.text(plain)));
        event.message(recolored);
    }
}
