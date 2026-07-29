package dev.iyanz.sessentials.util;

/**
 * Converts ASCII letters to their Unicode "small capitals" glyphs (e.g.
 * {@code "Bank"} &rarr; {@code "ʙᴀɴᴋ"}), used throughout SourbyBank's menus, logs
 * and messages for a consistent typographic style.
 *
 * <p>Only letters {@code A-Z}/{@code a-z} are transformed; digits, punctuation and
 * whitespace pass through untouched. The converter is <strong>not</strong> aware of
 * MiniMessage markup: never pass a string that already contains {@code <...>} tags
 * or legacy {@code &} codes, or their letters will be mangled. Convert the plain
 * text first, then wrap it in colour tags.</p>
 */
public final class SmallCaps {

    // Indexed a..z. 'x' and a couple of others have no dedicated small-cap glyph in
    // Unicode; the closest conventional substitutes are used.
    private static final String[] LOWER = {
            "ᴀ", // a ᴀ
            "ʙ", // b ʙ
            "ᴄ", // c ᴄ
            "ᴅ", // d ᴅ
            "ᴇ", // e ᴇ
            "ꜰ", // f ꜰ
            "ɢ", // g ɢ
            "ʜ", // h ʜ
            "ɪ", // i ɪ
            "ᴊ", // j ᴊ
            "ᴋ", // k ᴋ
            "ʟ", // l ʟ
            "ᴍ", // m ᴍ
            "ɴ", // n ɴ
            "ᴏ", // o ᴏ
            "ᴘ", // p ᴘ
            "ꞯ", // q ꞯ
            "ʀ", // r ʀ
            "ꜱ", // s ꜱ
            "ᴛ", // t ᴛ
            "ᴜ", // u ᴜ
            "ᴠ", // v ᴠ
            "ᴡ", // w ᴡ
            "x",       // x (no small-cap glyph)
            "ʏ", // y ʏ
            "ᴢ"  // z ᴢ
    };

    private SmallCaps() {
    }

    /**
     * Returns {@code input} with every ASCII letter replaced by its small-capital
     * glyph.
     *
     * @param input plain text (must not contain MiniMessage tags or {@code &} codes)
     * @return the small-caps rendering, or {@code ""} if {@code input} is {@code null}
     */
    public static String of(String input) {
        if (input == null) {
            return "";
        }
        final StringBuilder out = new StringBuilder(input.length() + 8);
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            if (c >= 'a' && c <= 'z') {
                out.append(LOWER[c - 'a']);
            } else if (c >= 'A' && c <= 'Z') {
                out.append(LOWER[c - 'A']);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
