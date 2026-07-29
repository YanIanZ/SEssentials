package dev.iyanz.sessentials.module.armoreffects;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.iyanz.sessentials.SEssentialsPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.potion.PotionEffectType;

/**
 * In-memory, reloadable view of the {@code armor-effects} config section: whether the
 * feature is globally enabled and the configured {@link ArmorEffectRule}s.
 *
 * <p>Expected {@code config.yml} shape:</p>
 * <pre>{@code
 * armor-effects:
 *   enabled: true
 *   effects:
 *     - material: NETHERITE_CHESTPLATE
 *       effect: FIRE_RESISTANCE
 *       amplifier: 0
 *     - material: GOLDEN_BOOTS
 *       effect: SPEED
 *       amplifier: 1
 * }</pre>
 *
 * <p>Built once on module {@link ArmorEffectsModule#enable}, and rebuilt in place by
 * {@link #reload(FileConfiguration)} on {@code /armoreffects reload}. Reads are
 * lock-free and safe from any region thread while a reload is in flight elsewhere: the
 * backing fields are {@code volatile} and each reload builds a fresh immutable list
 * before publishing it.</p>
 */
final class ArmorEffectsSettings {

    private static final String CONFIG_ROOT = "armor-effects";

    /** Demonstrative rules seeded into {@code config.yml} when none are configured. */
    private static final String DEFAULT_CHESTPLATE_MATERIAL = "NETHERITE_CHESTPLATE";
    private static final String DEFAULT_CHESTPLATE_EFFECT = "FIRE_RESISTANCE";
    private static final int DEFAULT_CHESTPLATE_AMPLIFIER = 0;
    private static final String DEFAULT_BOOTS_MATERIAL = "GOLDEN_BOOTS";
    private static final String DEFAULT_BOOTS_EFFECT = "SPEED";
    private static final int DEFAULT_BOOTS_AMPLIFIER = 1;

    private volatile boolean enabled;
    private volatile List<ArmorEffectRule> rules;

    private ArmorEffectsSettings() {
    }

    /** Builds a settings instance by reading {@code armor-effects} from {@code config}. */
    static ArmorEffectsSettings load(FileConfiguration config) {
        ArmorEffectsSettings settings = new ArmorEffectsSettings();
        settings.reload(config);
        return settings;
    }

    /**
     * Re-reads the {@code armor-effects} section from {@code config} and atomically
     * replaces the in-memory enabled flag and rule list.
     *
     * @param config the plugin's current configuration (call {@code plugin.reloadConfig()}
     *               beforehand to pick up on-disk edits)
     */
    void reload(FileConfiguration config) {
        boolean newEnabled = config.getBoolean(CONFIG_ROOT + ".enabled", true);

        List<ArmorEffectRule> loaded = new ArrayList<>();
        List<Map<?, ?>> entries = config.getMapList(CONFIG_ROOT + ".effects");
        for (Map<?, ?> entry : entries) {
            ArmorEffectRule rule = parseRule(entry);
            if (rule != null) {
                loaded.add(rule);
            }
        }

        this.enabled = newEnabled;
        this.rules = List.copyOf(loaded);
    }

    /** Parses one {@code effects} entry, or returns {@code null} if it is invalid. */
    private static ArmorEffectRule parseRule(Map<?, ?> entry) {
        Object materialRaw = entry.get("material");
        Object effectRaw = entry.get("effect");
        if (materialRaw == null || effectRaw == null) {
            return null;
        }

        Material material = Material.matchMaterial(materialRaw.toString());
        if (material == null) {
            return null;
        }

        PotionEffectType effect = resolveEffectType(effectRaw.toString());
        if (effect == null) {
            return null;
        }

        Object amplifierRaw = entry.get("amplifier");
        int amplifier = amplifierRaw instanceof Number number ? Math.max(0, number.intValue()) : 0;

        return new ArmorEffectRule(material, effect, amplifier);
    }

    /**
     * Resolves a potion effect type by name, first via {@link Registry#EFFECT} (Paper
     * 1.21's namespaced lookup, e.g. {@code "fire_resistance"}/{@code "FIRE_RESISTANCE"})
     * and falling back to the legacy {@link PotionEffectType#getByName(String)} for
     * spellings that are not valid namespaced-key characters.
     *
     * @param name the configured effect name
     * @return the resolved type, or {@code null} if neither lookup matches
     */
    @SuppressWarnings("deprecation")
    private static PotionEffectType resolveEffectType(String name) {
        String normalized = name.toLowerCase(Locale.ROOT).trim();
        try {
            PotionEffectType byKey = Registry.EFFECT.get(NamespacedKey.minecraft(normalized));
            if (byKey != null) {
                return byKey;
            }
        } catch (IllegalArgumentException notANamespacedKey) {
            // Fall through to the legacy name lookup below.
        }
        return PotionEffectType.getByName(name);
    }

    /**
     * Writes the two demonstrative rules described in this class's Javadoc into the
     * plugin config if {@code armor-effects.effects} is unset or empty, then saves the
     * file so operators immediately see and can edit a working example. No-op if any
     * rule is already configured.
     *
     * @param plugin the owning plugin, whose config is mutated and persisted
     */
    static void seedDefaultIfEmpty(SEssentialsPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        List<Map<?, ?>> existing = config.getMapList(CONFIG_ROOT + ".effects");
        if (!existing.isEmpty()) {
            return;
        }
        if (!config.contains(CONFIG_ROOT + ".enabled")) {
            config.set(CONFIG_ROOT + ".enabled", true);
        }
        config.set(CONFIG_ROOT + ".effects", List.of(
                defaultEntry(DEFAULT_CHESTPLATE_MATERIAL, DEFAULT_CHESTPLATE_EFFECT, DEFAULT_CHESTPLATE_AMPLIFIER),
                defaultEntry(DEFAULT_BOOTS_MATERIAL, DEFAULT_BOOTS_EFFECT, DEFAULT_BOOTS_AMPLIFIER)));
        plugin.saveConfig();
    }

    /** Builds one {@code effects} list entry as an ordered map, matching the YAML shape. */
    private static Map<String, Object> defaultEntry(String material, String effect, int amplifier) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("material", material);
        entry.put("effect", effect);
        entry.put("amplifier", amplifier);
        return entry;
    }

    /** @return whether the armor-effects feature is globally enabled. */
    boolean enabled() {
        return enabled;
    }

    /** @return every configured armor-effect rule. */
    List<ArmorEffectRule> rules() {
        return rules;
    }
}
