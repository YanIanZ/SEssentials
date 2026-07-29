package dev.iyanz.sessentials.module.emoji;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.iyanz.sessentials.SEssentialsPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * The immutable, in-memory view of the {@code emoji.map} config section: an ordered
 * mapping of short {@code code}s (e.g. {@code smile}) to the Unicode emoji strings they
 * expand to (e.g. {@code 😊}), plus the {@code :code:}-token replacement used by the
 * chat listener.
 *
 * <p>Expected {@code config.yml} shape:</p>
 * <pre>{@code
 * emoji:
 *   enabled: true
 *   map:
 *     smile: "😊"
 *     heart: "❤"
 *     star: "⭐"
 * }</pre>
 *
 * <p>Codes are matched case-insensitively (lower-cased on load and lookup). Only
 * {@code :code:} tokens whose code is present in this map are expanded; unknown tokens
 * such as {@code :foo:} are left untouched so ordinary text (URLs, timestamps, other
 * plugins' emote syntax) is never mangled.</p>
 */
final class EmojiMap {

    /** Matches a {@code :code:} token whose code is an identifier (letters, digits, {@code _}). */
    private static final Pattern TOKEN = Pattern.compile(":([A-Za-z0-9_]+):");

    /** The demonstrative default set seeded into a fresh {@code config.yml}. */
    private static final Map<String, String> DEFAULTS = defaults();

    /** Ordered {@code code -> emoji} view; keys are always lower-case. Unmodifiable. */
    private final Map<String, String> emojis;

    private EmojiMap(Map<String, String> emojis) {
        this.emojis = Collections.unmodifiableMap(emojis);
    }

    /**
     * Reads the {@code emoji.map} section from {@code config} into an immutable map.
     * Blank or missing emoji values are skipped; keys are lower-cased.
     *
     * @param config the plugin's current configuration
     * @return a populated {@link EmojiMap} (possibly empty)
     */
    static EmojiMap load(FileConfiguration config) {
        Map<String, String> loaded = new LinkedHashMap<>();
        ConfigurationSection section = config.getConfigurationSection("emoji.map");
        if (section != null) {
            for (String code : section.getKeys(false)) {
                String emoji = section.getString(code);
                if (emoji == null || emoji.isBlank()) {
                    continue;
                }
                loaded.put(code.toLowerCase(Locale.ROOT), emoji);
            }
        }
        return new EmojiMap(loaded);
    }

    /**
     * Ensures the plugin config carries an {@code emoji.enabled} flag and a non-empty
     * {@code emoji.map}: if either is absent, the missing part is seeded with the
     * built-in default set and the file is saved so operators immediately see and can
     * edit a working example. No-op when both are already present.
     *
     * @param plugin the owning plugin, whose config is mutated and persisted
     */
    static void seedDefaultsIfEmpty(SEssentialsPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        boolean dirty = false;

        if (!config.contains("emoji.enabled")) {
            config.set("emoji.enabled", true);
            dirty = true;
        }

        ConfigurationSection section = config.getConfigurationSection("emoji.map");
        if (section == null || section.getKeys(false).isEmpty()) {
            config.set("emoji.map", new LinkedHashMap<>(DEFAULTS));
            dirty = true;
        }

        if (dirty) {
            plugin.saveConfig();
        }
    }

    /**
     * Replaces every {@code :code:} token in {@code text} whose code is known with its
     * emoji, leaving unknown tokens and all other text verbatim.
     *
     * @param text the plain chat text
     * @return the expanded text, or the same reference if nothing was replaced
     */
    String replaceTokens(String text) {
        if (emojis.isEmpty()) {
            return text;
        }
        Matcher matcher = TOKEN.matcher(text);
        StringBuilder out = null;
        while (matcher.find()) {
            String emoji = emojis.get(matcher.group(1).toLowerCase(Locale.ROOT));
            if (emoji == null) {
                continue; // unknown code: leave the token in place
            }
            if (out == null) {
                out = new StringBuilder(text.length());
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(emoji));
        }
        if (out == null) {
            return text; // no known token matched
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /** @return {@code true} if no usable emoji are configured. */
    boolean isEmpty() {
        return emojis.isEmpty();
    }

    /** @return the ordered {@code code -> emoji} entries (unmodifiable). */
    Map<String, String> entries() {
        return emojis;
    }

    /** @return the built-in default {@code code -> emoji} set, in display order. */
    private static Map<String, String> defaults() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("smile", "😊");
        map.put("heart", "❤");
        map.put("star", "⭐");
        map.put("fire", "🔥");
        map.put("check", "✔");
        map.put("cross", "✖");
        map.put("skull", "☠");
        map.put("arrow", "➜");
        return map;
    }
}
