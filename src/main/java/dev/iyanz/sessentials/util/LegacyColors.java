package dev.iyanz.sessentials.util;

/**
 * Converts legacy Bukkit colour codes to <a href="https://docs.advntr.dev/minimessage/">MiniMessage</a>
 * tags so operators may paste either style into configuration files.
 *
 * <p>Supported conversions:</p>
 * <ul>
 *   <li>{@code &0}-{@code &9}, {@code &a}-{@code &f} — colours</li>
 *   <li>{@code &k}-{@code &o}, {@code &r} — decorations / reset</li>
 *   <li>{@code &#RRGGBB} — hex colour ({@code <#RRGGBB>})</li>
 *   <li>{@code &x&R&R&G&G&B&B} — Bungee/Spigot hex form ({@code <#RRGGBB>})</li>
 * </ul>
 *
 * <p>The section sign {@code §} is treated exactly like {@code &}. Text that is
 * already MiniMessage (using {@code <...>} tags) passes through untouched, so the
 * function is idempotent for pure-MiniMessage input. This is a pure function with
 * no side effects, making it directly unit-testable.</p>
 */
public final class LegacyColors {

    private LegacyColors() {
    }

    /**
     * Converts any legacy {@code &}/{@code §} colour codes in {@code input} to MiniMessage tags.
     *
     * @param input the source string; may already be MiniMessage or contain legacy codes
     * @return an equivalent MiniMessage string, or {@code ""} if {@code input} is {@code null}
     */
    public static String toMiniMessage(String input) {
        if (input == null) {
            return "";
        }
        if (input.isEmpty()) {
            return input;
        }
        final int n = input.length();
        final StringBuilder out = new StringBuilder(n + 16);
        int i = 0;
        while (i < n) {
            final char c = input.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < n) {
                final char code = input.charAt(i + 1);

                // &#RRGGBB
                if (code == '#' && isHex6(input, i + 2)) {
                    out.append("<#").append(input, i + 2, i + 8).append('>');
                    i += 8;
                    continue;
                }

                // &x&R&R&G&G&B&B  (Bungee/Spigot hex form)
                if ((code == 'x' || code == 'X') && isBungeeHex(input, i)) {
                    out.append("<#");
                    for (int k = 0; k < 6; k++) {
                        out.append(input.charAt(i + 3 + (k * 2)));
                    }
                    out.append('>');
                    i += 14;
                    continue;
                }

                final String tag = tagFor(code);
                if (tag != null) {
                    out.append(tag);
                    i += 2;
                    continue;
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static boolean isHex6(String s, int start) {
        if (start + 6 > s.length()) {
            return false;
        }
        for (int k = 0; k < 6; k++) {
            if (!isHex(s.charAt(start + k))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isBungeeHex(String s, int amp) {
        // Pattern spans 14 chars from the leading '&': &x&R&R&G&G&B&B
        if (amp + 14 > s.length()) {
            return false;
        }
        for (int k = 0; k < 6; k++) {
            final char sep = s.charAt(amp + 2 + (k * 2));
            final char hex = s.charAt(amp + 3 + (k * 2));
            if ((sep != '&' && sep != '§') || !isHex(hex)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static String tagFor(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset>";
            default -> null;
        };
    }
}
