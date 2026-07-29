package dev.iyanz.sessentials.module.ctext;

import java.util.ArrayList;
import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Reads operator-defined rich messages from the plugin config's {@code ctexts}
 * section. Each key maps to either a single MiniMessage string or a list of
 * MiniMessage lines:
 *
 * <pre>
 * ctexts:
 *   rules:                       # list-valued entry (one line per element)
 *     - "&lt;gold&gt;Server Rules"
 *     - "&lt;gray&gt;1. Be nice."
 *   discord: "&lt;click:open_url:'...'&gt;&lt;aqua&gt;Join our Discord!"   # single-string entry
 * </pre>
 *
 * <p>Values are always read live from {@link SEssentialsPlugin#getConfig()}, so a
 * {@code /ctext reload} ({@link SEssentialsPlugin#reloadConfig()}) takes effect without
 * any cached state to refresh here.</p>
 */
final class CTexts {

    /** Root config section holding every named ctext. */
    static final String SECTION = "ctexts";

    private final SEssentialsPlugin plugin;

    /**
     * @param plugin the owning plugin, whose live config backs every lookup
     */
    CTexts(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Writes a demonstrative {@code rules} ctext into the config if the {@code ctexts}
     * section is unset or empty, then saves the file so operators immediately see a
     * working, editable example. No-op if any ctext is already defined.
     *
     * @param plugin the owning plugin, whose config is mutated and persisted
     */
    static void seedDefaultIfEmpty(SEssentialsPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection(SECTION);
        if (section != null && !section.getKeys(false).isEmpty()) {
            return;
        }
        config.set(SECTION + ".rules", List.of("<gold>Server Rules", "<gray>1. Be nice."));
        plugin.saveConfig();
    }

    /** @return the names of every defined ctext (the {@code ctexts} section's keys), never {@code null}. */
    List<String> names() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(SECTION);
        return section == null ? List.of() : new ArrayList<>(section.getKeys(false));
    }

    /**
     * Resolves a named ctext to its rendered MiniMessage lines. Both single-string and
     * list-valued entries are supported; a string yields a single line.
     *
     * @param name the ctext name
     * @return the rendered lines in order, or an empty list if {@code name} is undefined
     *         (or defined but blank)
     */
    List<Component> render(String name) {
        FileConfiguration config = plugin.getConfig();
        String path = SECTION + "." + name;
        if (!config.contains(path)) {
            return List.of();
        }
        List<String> raw;
        if (config.isList(path)) {
            raw = config.getStringList(path);
        } else {
            String single = config.getString(path);
            raw = single == null ? List.of() : List.of(single);
        }
        List<Component> lines = new ArrayList<>(raw.size());
        for (String line : raw) {
            lines.add(Msg.mm(line));
        }
        return lines;
    }
}
