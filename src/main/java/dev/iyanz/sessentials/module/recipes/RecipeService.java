package dev.iyanz.sessentials.module.recipes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.store.YamlStore;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

/**
 * Loads custom crafting recipes from the {@code recipes} data store
 * ({@code recipes.yml}) and (un)registers them with the server.
 *
 * <p>Each entry under {@code recipes.<key>} is either a {@code shaped} or
 * {@code shapeless} recipe:</p>
 * <pre>
 * recipes:
 *   &lt;key&gt;:
 *     type: shaped        # or shapeless
 *     result: DIAMOND
 *     amount: 1
 *     shape: ["ABA","BAB","ABA"]              # shaped only
 *     ingredients: { A: IRON_INGOT, B: GOLD_INGOT }   # shaped only
 *     materials: [OAK_LOG, OAK_LOG]            # shapeless only
 * </pre>
 *
 * <p>Every recipe is registered under the namespaced key {@code sessentials:recipe_<key>}
 * (built as {@code new NamespacedKey(plugin, "recipe_" + key)}). Registration always runs
 * on Folia's {@link Bukkit#getGlobalRegionScheduler() global region scheduler}, since recipe
 * tables are server-global state, not tied to any single region. A malformed entry is
 * skipped with a logged warning so it never prevents the rest from loading.</p>
 */
final class RecipeService {

    private static final String ROOT = "recipes";
    private static final String KEY_PREFIX = "recipe_";

    /** The demonstrable example seeded into an empty store on first run. */
    private static final String EXAMPLE_KEY = "iron_to_diamond";

    private final YamlStore store;
    private final List<NamespacedKey> registeredKeys = new ArrayList<>();

    RecipeService(YamlStore store) {
        this.store = store;
    }

    /**
     * Seeds one demonstrable example recipe into the store if no recipes have been
     * defined yet (i.e. this is the plugin's first run with an empty recipe store).
     * No-op otherwise. Compresses a full stack of iron ingots (shaped, 3x3) into a
     * single diamond, showcasing the shaped recipe format.
     */
    void ensureDefaults() {
        if (!store.keys(ROOT).isEmpty()) {
            return;
        }
        String base = ROOT + "." + EXAMPLE_KEY;
        store.set(base + ".type", "shaped");
        store.set(base + ".result", "DIAMOND");
        store.set(base + ".amount", 1);
        store.set(base + ".shape", List.of("III", "III", "III"));
        store.set(base + ".ingredients.I", "IRON_INGOT");
        store.save();
    }

    /**
     * (Re-)registers every recipe defined in the store with the server, on Folia's
     * global region scheduler. Any previously-registered recipes from this service are
     * left untouched by this call; use {@link #reload(SEssentialsPlugin, CommandSender)}
     * to unregister first.
     *
     * @param plugin the owning plugin
     */
    void registerAll(SEssentialsPlugin plugin) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> loadAll(plugin));
    }

    /**
     * Unregisters every recipe this service previously registered, then re-registers
     * every recipe currently defined in the store, reporting the outcome to
     * {@code sender}. Runs entirely on Folia's global region scheduler so the
     * unregister/register cycle (and the resulting count) is atomic with respect to
     * other recipe-table work.
     *
     * @param plugin the owning plugin
     * @param sender who to notify once the reload completes
     */
    void reload(SEssentialsPlugin plugin, CommandSender sender) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            for (NamespacedKey key : registeredKeys) {
                Bukkit.removeRecipe(key);
            }
            registeredKeys.clear();
            loadAll(plugin);
            Msg.ok(sender, "Custom recipes reloaded (" + registeredKeys.size() + " active).");
        });
    }

    /** Loads and registers every recipe currently defined in the store. Must run on the global region thread. */
    private void loadAll(SEssentialsPlugin plugin) {
        for (String key : store.keys(ROOT)) {
            try {
                registerOne(plugin, key);
            } catch (Exception ex) {
                plugin.getLogger().warning("Skipping invalid recipe \"" + key + "\": " + ex.getMessage());
            }
        }
    }

    /** Parses and registers a single recipe entry. Throws on any malformed field. */
    private void registerOne(SEssentialsPlugin plugin, String key) {
        ConfigurationSection section = store.config().getConfigurationSection(ROOT + "." + key);
        if (section == null) {
            throw new IllegalArgumentException("no configuration section");
        }
        String type = section.getString("type", "");
        String resultName = section.getString("result");
        if (resultName == null || resultName.isBlank()) {
            throw new IllegalArgumentException("missing 'result'");
        }
        Material resultMaterial = Material.matchMaterial(resultName);
        if (resultMaterial == null) {
            throw new IllegalArgumentException("unknown result material \"" + resultName + "\"");
        }
        int amount = Math.max(1, section.getInt("amount", 1));
        NamespacedKey namespacedKey = new NamespacedKey(plugin, KEY_PREFIX + key);
        ItemStack result = new ItemStack(resultMaterial, amount);

        Recipe recipe = switch (type.toLowerCase(Locale.ROOT)) {
            case "shaped" -> buildShaped(section, namespacedKey, result);
            case "shapeless" -> buildShapeless(section, namespacedKey, result);
            default -> throw new IllegalArgumentException("unknown recipe type \"" + type + "\" (use shaped/shapeless)");
        };

        Bukkit.addRecipe(recipe);
        registeredKeys.add(namespacedKey);
    }

    /** Builds a {@link ShapedRecipe} from a {@code shape}/{@code ingredients} config entry. */
    private ShapedRecipe buildShaped(ConfigurationSection section, NamespacedKey key, ItemStack result) {
        List<String> shape = section.getStringList("shape");
        if (shape.isEmpty()) {
            throw new IllegalArgumentException("shaped recipe missing 'shape'");
        }
        ConfigurationSection ingredients = section.getConfigurationSection("ingredients");
        if (ingredients == null || ingredients.getKeys(false).isEmpty()) {
            throw new IllegalArgumentException("shaped recipe missing 'ingredients'");
        }

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(shape.toArray(new String[0]));
        for (String symbol : ingredients.getKeys(false)) {
            if (symbol.length() != 1) {
                throw new IllegalArgumentException("ingredient key \"" + symbol + "\" must be a single character");
            }
            String materialName = ingredients.getString(symbol);
            Material material = materialName == null ? null : Material.matchMaterial(materialName);
            if (material == null) {
                throw new IllegalArgumentException("unknown ingredient material \"" + materialName + "\" for key '" + symbol + "'");
            }
            recipe.setIngredient(symbol.charAt(0), material);
        }
        return recipe;
    }

    /** Builds a {@link ShapelessRecipe} from a {@code materials} config entry. */
    private ShapelessRecipe buildShapeless(ConfigurationSection section, NamespacedKey key, ItemStack result) {
        List<String> materials = section.getStringList("materials");
        if (materials.isEmpty()) {
            throw new IllegalArgumentException("shapeless recipe missing 'materials'");
        }
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        for (String materialName : materials) {
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                throw new IllegalArgumentException("unknown material \"" + materialName + "\"");
            }
            recipe.addIngredient(material);
        }
        return recipe;
    }
}
