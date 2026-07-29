package dev.iyanz.sessentials.module.recipes;

import dev.iyanz.sessentials.SEssentialsPlugin;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Registers the {@code /recipes} command tree: {@code /recipes reload} unregisters and
 * re-registers every custom recipe defined in the {@code recipes} data store, gated by
 * {@code sessentials.recipes}.
 */
@SuppressWarnings("UnstableApiUsage")
final class RecipeCommands {

    private final SEssentialsPlugin plugin;
    private final RecipeService service;

    RecipeCommands(SEssentialsPlugin plugin, RecipeService service) {
        this.plugin = plugin;
        this.service = service;
    }

    /** Builds and registers {@code /recipes} on the command lifecycle. */
    void register() {
        plugin.commands(reg -> reg.register(Commands.literal("recipes")
                .requires(s -> s.getSender().hasPermission("sessentials.recipes"))
                .then(Commands.literal("reload").executes(ctx -> {
                    service.reload(plugin, ctx.getSource().getSender());
                    return 1;
                }))
                .build(), "Manage custom crafting recipes"));
    }
}
