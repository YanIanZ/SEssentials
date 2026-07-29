package dev.iyanz.sessentials.module.recipes;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * The recipes feature module: config-driven custom crafting recipes, defined in the
 * {@code recipes} data store ({@code recipes.yml}) and registered with the server on
 * enable. {@code /recipes reload} (permission {@code sessentials.recipes}) unregisters
 * and re-registers every recipe this module owns, from {@link RecipeCommands}.
 *
 * <p>On a fresh install (empty recipe store), a demonstrative shaped recipe is seeded
 * automatically by {@link RecipeService#ensureDefaults()}.</p>
 */
public final class RecipesModule implements EssModule {

    @Override
    public String name() {
        return "recipes";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        RecipeService service = new RecipeService(plugin.stores().get("recipes"));
        service.ensureDefaults();
        service.registerAll(plugin);

        new RecipeCommands(plugin, service).register();
    }
}
