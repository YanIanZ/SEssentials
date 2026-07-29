package dev.iyanz.sessentials.util;

/**
 * Central visual theme for SourbyBank: the colour palette (hex gradients + solid
 * accents) plus small helpers that combine those colours with {@link SmallCaps}
 * text. Keeping every colour in one place means the whole plugin — menus, console
 * banner and chat messages — shares a single, tweakable look.
 *
 * <p>All constants are MiniMessage fragments. Helper methods small-caps the human
 * text <em>before</em> wrapping it in tags, so the tags themselves are never
 * corrupted.</p>
 */
public final class Style {

    /** Warm-gold brand gradient used for titles and the coin symbol. */
    public static final String BRAND = "<gradient:#FFE29F:#FFA751>";
    /** Bold brand gradient for menu / banner titles. */
    public static final String TITLE = "<gradient:#F9D976:#E0A526><bold>";
    /** Positive / deposit green gradient. */
    public static final String DEPOSIT = "<gradient:#A8E063:#56AB2F>";
    /** Withdraw amber gradient. */
    public static final String WITHDRAW = "<gradient:#F7971E:#FFD200>";
    /** Danger / close red gradient. */
    public static final String DANGER = "<gradient:#FF512F:#F09819>";
    /** Informational teal-to-sky gradient. */
    public static final String INFO = "<gradient:#43C6AC:#4FC3F7>";
    /** Upgrade purple gradient. */
    public static final String UPGRADE = "<gradient:#B24592:#8E2DE2>";
    /** History / neutral blue gradient. */
    public static final String HISTORY = "<gradient:#4E9BE0:#6DD5FA>";
    /** Coin-value gradient (numbers). */
    public static final String COIN = "<gradient:#FFD86B:#F7B733>";

    /** Muted grey for descriptive lore. */
    public static final String GRAY = "<#9AA0A6>";
    /** Near-white for emphasised values. */
    public static final String VALUE = "<#F5F5F5>";
    /** Soft yellow for "click to…" hints. */
    public static final String HINT = "<#FFD54F>";
    /** Success green (chat). */
    public static final String OK = "<#8BE28B>";
    /** Error red (chat). */
    public static final String BAD = "<#FF7B7B>";

    private Style() {
    }

    /**
     * A bold, small-capped title in the brand gradient (for menu titles).
     *
     * @param text plain title text
     * @return a MiniMessage string
     */
    public static String title(String text) {
        return TITLE + SmallCaps.of(text);
    }

    /**
     * A small-capped button name in the given colour fragment.
     *
     * @param color a palette colour constant (e.g. {@link #DEPOSIT})
     * @param text  plain button label
     * @return a MiniMessage string
     */
    public static String button(String color, String text) {
        return color + "<bold>" + SmallCaps.of(text);
    }

    /**
     * A grey small-capped lore line.
     *
     * @param text plain lore text
     * @return a MiniMessage string
     */
    public static String lore(String text) {
        return GRAY + SmallCaps.of(text);
    }

    /**
     * A grey label followed by a coin-coloured value: {@code "Balance: 1,234"}.
     *
     * @param label plain label text (small-capped)
     * @param value pre-formatted value (kept verbatim)
     * @return a MiniMessage string
     */
    public static String labelled(String label, String value) {
        return GRAY + SmallCaps.of(label) + " " + COIN + value;
    }

    /**
     * A soft-yellow small-capped "click to…" hint line.
     *
     * @param text plain hint text
     * @return a MiniMessage string
     */
    public static String hint(String text) {
        return HINT + SmallCaps.of(text);
    }
}
