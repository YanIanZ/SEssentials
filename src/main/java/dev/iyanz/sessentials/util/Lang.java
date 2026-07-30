package dev.iyanz.sessentials.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Loads and resolves {@code messages.yml}, the plugin's customizable language file for the
 * SHARED, cross-cutting messages that many modules emit (permission errors, "player not
 * found", cooldowns, and so on). Admins edit the file in the plugin data folder to re-word
 * or re-colour those messages without touching code.
 *
 * <p>Each message lives under a dotted key (e.g. {@code common.player-not-found}) and may
 * contain MiniMessage tags and/or legacy {@code &}/{@code §} colour codes plus
 * {@code {token}} placeholders. This class only performs the {@code {token}} → value
 * substitution and returns the raw string; colour parsing is left to {@link Msg} (via
 * {@link Msg#mm}) at send time, so a caller keeps full control over rendering.</p>
 *
 * <p>Resolution order for {@link #get}: the admin-edited file, then the bundled default
 * shipped in the jar, then the key itself — so a missing or mistyped key is always visible
 * rather than silently blank.</p>
 *
 * <p><strong>Thread-safety.</strong> The backing configurations are read-only after load
 * and the two references are {@code volatile}; {@link #reload} swaps them atomically, so
 * lookups from Folia region threads never observe a half-loaded file.</p>
 */
public final class Lang {

    private static final String FILE_NAME = "messages.yml";

    /** Admin-editable messages loaded from the data folder. Swapped atomically on reload. */
    private static volatile FileConfiguration messages;
    /** Bundled defaults read from the jar; the fallback for keys the admin removed. */
    private static volatile FileConfiguration defaults;

    private Lang() {
    }

    /**
     * Copies the bundled {@code messages.yml} into the data folder on first run and loads
     * it. Call once from {@code onEnable}.
     *
     * @param plugin the owning plugin
     */
    public static void init(Plugin plugin) {
        load(plugin);
    }

    /**
     * Re-reads {@code messages.yml} from disk (recreating it from the bundled copy if it was
     * deleted). Safe to call at runtime, e.g. from a {@code /reload} command.
     *
     * @param plugin the owning plugin
     */
    public static void reload(Plugin plugin) {
        load(plugin);
    }

    private static void load(Plugin plugin) {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
        try (InputStream in = plugin.getResource(FILE_NAME)) {
            defaults = in == null ? null
                    : YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            defaults = null;
            plugin.getLogger().warning("Could not read bundled " + FILE_NAME + ": " + ex.getMessage());
        }
    }

    /**
     * Resolves a message by key, substituting {@code {token}} placeholders.
     *
     * <p>Placeholder pairs are supplied as alternating token/value arguments, e.g.
     * {@code Lang.get("common.player-not-found", "player", name)} replaces {@code {player}}
     * with {@code name}. Values are converted with {@link String#valueOf}; a trailing
     * unpaired argument is ignored.</p>
     *
     * @param key              the dotted message key (e.g. {@code common.no-permission})
     * @param placeholderPairs alternating token names and their replacement values
     * @return the resolved message (admin value, else bundled default, else the key itself),
     *         with placeholders substituted
     */
    public static String get(String key, Object... placeholderPairs) {
        FileConfiguration cfg = messages;
        String raw = cfg == null ? null : cfg.getString(key);
        if (raw == null) {
            FileConfiguration def = defaults;
            raw = def == null ? null : def.getString(key);
        }
        if (raw == null) {
            raw = key;
        }
        return substitute(raw, placeholderPairs);
    }

    private static String substitute(String template, Object... pairs) {
        if (pairs == null || pairs.length < 2) {
            return template;
        }
        String result = template;
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            result = result.replace("{" + pairs[i] + "}", String.valueOf(pairs[i + 1]));
        }
        return result;
    }
}
