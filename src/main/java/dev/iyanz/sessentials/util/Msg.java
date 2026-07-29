package dev.iyanz.sessentials.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

/**
 * Dead-simple, project-wide messaging helper. Modules call {@link #ok}, {@link #err}
 * and {@link #info} with plain text; the text is rendered in Small Caps with the
 * shared {@link Style} palette and a configurable prefix. Raw MiniMessage is also
 * available via {@link #mm} for richer messages.
 */
public final class Msg {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static String prefix = "";

    private Msg() {
    }

    /**
     * Sets the prefix (MiniMessage) prepended to {@link #ok}/{@link #err}/{@link #info}.
     *
     * @param miniMessagePrefix the prefix, in MiniMessage / legacy form
     */
    public static void init(String miniMessagePrefix) {
        prefix = LegacyColors.toMiniMessage(miniMessagePrefix == null ? "" : miniMessagePrefix);
    }

    /** Parses a MiniMessage string (legacy codes accepted) into a component. */
    public static Component mm(String miniMessage, TagResolver... resolvers) {
        return MM.deserialize(LegacyColors.toMiniMessage(miniMessage), resolvers);
    }

    /** Sends a raw MiniMessage line (no prefix, no small-caps transform). */
    public static void raw(CommandSender to, String miniMessage) {
        to.sendMessage(mm(miniMessage));
    }

    /** Prefixed success line: green Small Caps. */
    public static void ok(CommandSender to, String plainText) {
        to.sendMessage(mm(prefix + Style.OK + SmallCaps.of(plainText)));
    }

    /** Prefixed error line: red Small Caps. */
    public static void err(CommandSender to, String plainText) {
        to.sendMessage(mm(prefix + Style.BAD + SmallCaps.of(plainText)));
    }

    /** Prefixed neutral line: grey Small Caps. */
    public static void info(CommandSender to, String plainText) {
        to.sendMessage(mm(prefix + Style.GRAY + SmallCaps.of(plainText)));
    }

    /**
     * Prefixed line that mixes small-capped label text with a highlighted value.
     *
     * @param to    recipient
     * @param label plain text (small-capped, grey)
     * @param value pre-formatted value (kept verbatim, coin-gold)
     */
    public static void value(CommandSender to, String label, String value) {
        to.sendMessage(mm(prefix + Style.GRAY + SmallCaps.of(label) + " " + Style.COIN + value));
    }

    /** @return the configured prefix as a MiniMessage fragment. */
    public static String prefix() {
        return prefix;
    }
}
