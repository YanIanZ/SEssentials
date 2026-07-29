package dev.iyanz.sessentials.module.emoji;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Expands {@code :code:} emoji tokens in chat messages using the shared {@link EmojiMap}.
 *
 * <p>Runs at {@link EventPriority#LOW} (with {@code ignoreCancelled = true}), strictly
 * ahead of the per-player chat-colour listener at {@code NORMAL}, and works purely on the
 * message's <strong>plain</strong> text: it re-derives the text via {@link
 * PlainTextComponentSerializer}, swaps known tokens for their emoji and — only when a
 * substitution actually occurred — re-installs the result as a literal {@link Component}.
 * Nothing the player types is ever re-parsed as markup, so chat can never inject colours
 * or click/hover events through this listener.</p>
 *
 * <p><strong>Composition with chat colour.</strong> The colour listener also operates on
 * plain text — it serializes {@link AsyncChatEvent#message()} to plain and re-wraps it in
 * the sender's colour on every pass. Because the emoji pass runs at {@code LOW} and the
 * colour pass at {@code NORMAL}, the emoji expansion always lands first and the colour
 * listener re-wraps the emoji-bearing text last, so the player's colour is preserved even
 * when the message contains an emoji token. A message with no known token is moreover left
 * completely untouched, so an already-coloured message is never flattened needlessly.</p>
 *
 * <p>Gated per player on {@code sessentials.emoji.use}.</p>
 */
public final class EmojiChatListener implements Listener {

    private final EmojiMap emojiMap;

    /**
     * @param emojiMap the shared code-to-emoji mapping
     */
    public EmojiChatListener(EmojiMap emojiMap) {
        this.emojiMap = emojiMap;
    }

    /** Expands known {@code :code:} tokens in the sender's message, if they may use emoji. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("sessentials.emoji.use")) {
            return;
        }
        String plain = PlainTextComponentSerializer.plainText().serialize(event.message());
        String replaced = emojiMap.replaceTokens(plain);
        if (!replaced.equals(plain)) {
            event.message(Component.text(replaced));
        }
    }
}
