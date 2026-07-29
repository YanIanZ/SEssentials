package dev.iyanz.sessentials.module.chatfilter;

import java.util.List;
import java.util.Locale;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Screens the plain text of every chat message against a configurable list of banned
 * words and either blocks or censors the message on a match.
 *
 * <p>Reads {@code chat-filter.enabled} (default {@code true}), {@code
 * chat-filter.banned-words} (a string list; a couple of built-in placeholder words are
 * used if it is unset or empty) and {@code chat-filter.action} ({@code "block"} or
 * {@code "censor"}, default {@code "censor"}) fresh on every message, so a config
 * reload takes effect immediately.</p>
 *
 * <p>Matching is a deliberately simple, case-insensitive substring check per banned
 * word ({@link String#contains}) against the message flattened to plain text via
 * {@link PlainTextComponentSerializer} — no regular expressions, so there is no risk of
 * catastrophic backtracking on attacker-controlled input.</p>
 *
 * <p>Runs at {@link EventPriority#LOW} with {@code ignoreCancelled = true}, i.e. right
 * after the moderation module's mute check (which runs at {@link EventPriority#LOWEST}
 * and cancels the event for muted senders) and before this plugin's own chat-colour and
 * chat-format listeners, so filtering always sees the sender's true original text and
 * a muted player's message is never wastefully scanned.</p>
 */
public final class ChatFilterListener implements Listener {

    /** Used when {@code chat-filter.banned-words} is unset or empty. */
    private static final List<String> DEFAULT_BANNED_WORDS = List.of("badword1", "badword2");

    /** Replaces each matched banned word when {@code chat-filter.action} is {@code censor}. */
    private static final String CENSOR_MASK = "***";

    private final SEssentialsPlugin plugin;

    /**
     * @param plugin the owning plugin, used for its live {@code chat-filter.*} config
     */
    public ChatFilterListener(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Blocks or censors the message if it contains a configured banned word. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat-filter.enabled", true)) {
            return;
        }
        List<String> bannedWords = plugin.getConfig().getStringList("chat-filter.banned-words");
        if (bannedWords.isEmpty()) {
            bannedWords = DEFAULT_BANNED_WORDS;
        }

        String plain = PlainTextComponentSerializer.plainText().serialize(event.message());
        String lowerPlain = plain.toLowerCase(Locale.ROOT);
        if (!containsAny(lowerPlain, bannedWords)) {
            return;
        }

        String action = plugin.getConfig().getString("chat-filter.action", "censor");
        if ("block".equalsIgnoreCase(action)) {
            event.setCancelled(true);
            Msg.err(event.getPlayer(), "Your message contains a banned word and was blocked.");
            return;
        }

        event.message(Component.text(censor(plain, bannedWords)));
    }

    /** @return {@code true} if the (already lower-cased) text contains any banned word. */
    private static boolean containsAny(String lowerPlain, List<String> bannedWords) {
        for (String word : bannedWords) {
            if (word != null && !word.isBlank() && lowerPlain.contains(word.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /** @return {@code plain} with every case-insensitive occurrence of each banned word masked. */
    private static String censor(String plain, List<String> bannedWords) {
        String result = plain;
        for (String word : bannedWords) {
            if (word != null && !word.isBlank()) {
                result = replaceCaseInsensitive(result, word);
            }
        }
        return result;
    }

    /** Replaces every case-insensitive occurrence of {@code target} in {@code text} with the censor mask. */
    private static String replaceCaseInsensitive(String text, String target) {
        String lowerText = text.toLowerCase(Locale.ROOT);
        String lowerTarget = target.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(text.length());
        int from = 0;
        int found;
        while ((found = lowerText.indexOf(lowerTarget, from)) >= 0) {
            out.append(text, from, found).append(CENSOR_MASK);
            from = found + lowerTarget.length();
        }
        out.append(text, from, text.length());
        return out.toString();
    }
}
