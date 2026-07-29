package dev.iyanz.sessentials.module.chat;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.store.YamlStore;

/**
 * Persists and validates each player's chosen chat colour.
 *
 * <p>A colour is stored as a single MiniMessage "opening" tag — e.g. {@code <red>} or
 * {@code <gradient:#43C6AC:#4FC3F7>} — in the {@code "chat"} data store, keyed by
 * {@code "<uuid>.color"}. {@link #normalize} only ever accepts a fixed vocabulary of
 * named colours, hex codes and {@code gradient:c1:c2} specs, so the resulting tag is
 * always a known-safe MiniMessage fragment; nothing derived from a player's free-form
 * chat text is ever fed back through this validator or through MiniMessage.</p>
 */
public final class ChatColorService {

    private static final String KEY_SUFFIX = ".color";

    private static final Set<String> NAMED_COLORS = Set.of(
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
            "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
            "yellow", "white");

    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9a-f]{6}");

    private final SEssentialsPlugin plugin;

    /**
     * @param plugin the owning plugin, used for its {@code "chat"} data store
     */
    public ChatColorService(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Validates and normalises a player-supplied colour spec into a storable
     * MiniMessage opening tag.
     *
     * @param rawInput the command argument, e.g. {@code "red"}, {@code "<red>"} or
     *                  {@code "gradient:#43C6AC:#4FC3F7"} (named colours are also
     *                  accepted as gradient stops)
     * @return the normalised tag, e.g. {@code "<red>"} or {@code "<gradient:#43c6ac:#4fc3f7>"}
     * @throws IllegalArgumentException if the spec is not a recognised colour or gradient
     */
    public String normalize(String rawInput) {
        String value = rawInput == null ? "" : rawInput.trim().toLowerCase(Locale.ROOT);
        if (value.length() > 1 && value.charAt(0) == '<' && value.charAt(value.length() - 1) == '>') {
            value = value.substring(1, value.length() - 1);
        }
        if (isColorToken(value)) {
            return "<" + value + ">";
        }
        if (value.startsWith("gradient:")) {
            String[] stops = value.split(":");
            if (stops.length < 3) {
                throw new IllegalArgumentException(
                        "A gradient needs at least two colours, e.g. gradient:#RRGGBB:#RRGGBB.");
            }
            for (int i = 1; i < stops.length; i++) {
                if (!isColorToken(stops[i])) {
                    throw invalid();
                }
            }
            return "<" + value + ">";
        }
        throw invalid();
    }

    /** Persists {@code tag} as {@code target}'s chat colour. */
    public void set(UUID target, String tag) {
        store().set(target + KEY_SUFFIX, tag);
        store().save();
    }

    /** Clears {@code target}'s persisted chat colour (back to server default). */
    public void clear(UUID target) {
        store().remove(target + KEY_SUFFIX);
        store().save();
    }

    /** @return the stored MiniMessage colour/gradient tag for {@code target}, or {@code null} if none is set. */
    public String colorOf(UUID target) {
        return store().getString(target + KEY_SUFFIX);
    }

    private boolean isColorToken(String token) {
        return NAMED_COLORS.contains(token) || HEX_COLOR.matcher(token).matches();
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException(
                "Not a recognised colour or gradient. Try a name (red, green, aqua, gold, ...) "
                        + "or gradient:#RRGGBB:#RRGGBB.");
    }

    private YamlStore store() {
        return plugin.stores().get("chat");
    }
}
