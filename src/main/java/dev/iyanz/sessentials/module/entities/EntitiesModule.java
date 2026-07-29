package dev.iyanz.sessentials.module.entities;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

/**
 * Server lag / entity diagnostics. Provides two administrative commands for
 * inspecting and pruning the entities loaded in a world:
 * <ul>
 *   <li>{@code /entities [world]} — a per-{@link EntityType} census of the world,
 *       useful for spotting entity build-up that hurts tick times.</li>
 *   <li>{@code /killentities <type> [world]} — bulk removal of a single entity
 *       type (never players), for clearing that build-up.</li>
 * </ul>
 *
 * <p>Both commands read or mutate a whole world's entity list, so the actual work
 * runs on Folia's {@link Bukkit#getGlobalRegionScheduler() global region scheduler}
 * (see {@link EntityOps}); the command executors here only resolve arguments and
 * dispatch.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class EntitiesModule implements EssModule {

    private SEssentialsPlugin plugin;

    @Override
    public String name() {
        return "entities";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.plugin = plugin;
        plugin.commands(reg -> {
            reg.register(Commands.literal("entities")
                    .requires(s -> s.getSender().hasPermission("sessentials.entities"))
                    .executes(ctx -> entities(ctx, null))
                    .then(Commands.argument("world", StringArgumentType.word()).suggests(this::worldSuggestions)
                            .executes(ctx -> entities(ctx, StringArgumentType.getString(ctx, "world"))))
                    .build(), "Count loaded entities per type in a world",
                    List.of("entitycount", "mobcount"));

            reg.register(Commands.literal("killentities")
                    .requires(s -> s.getSender().hasPermission("sessentials.entities.kill"))
                    .then(Commands.argument("type", StringArgumentType.word()).suggests(EntityTypeSuggestions.SUGGEST)
                            .executes(ctx -> kill(ctx, null))
                            .then(Commands.argument("world", StringArgumentType.word()).suggests(this::worldSuggestions)
                                    .executes(ctx -> kill(ctx, StringArgumentType.getString(ctx, "world")))))
                    .build(), "Remove all entities of a type in a world",
                    List.of("removeentities"));
        });
    }

    /** Runs the {@code /entities} census for the resolved world. */
    private int entities(CommandContext<CommandSourceStack> ctx, String worldName) {
        CommandSender sender = ctx.getSource().getSender();
        World world = resolveWorld(sender, worldName);
        if (world == null) {
            return 0;
        }
        EntityOps.count(plugin, sender, world);
        return 1;
    }

    /** Parses the entity type and runs the {@code /killentities} removal for the resolved world. */
    private int kill(CommandContext<CommandSourceStack> ctx, String worldName) {
        CommandSender sender = ctx.getSource().getSender();
        String raw = StringArgumentType.getString(ctx, "type");
        EntityType type;
        try {
            type = EntityType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            Msg.err(sender, "Unknown entity type: " + raw + ".");
            return 0;
        }
        if (type == EntityType.PLAYER) {
            Msg.err(sender, "Players cannot be removed.");
            return 0;
        }
        World world = resolveWorld(sender, worldName);
        if (world == null) {
            return 0;
        }
        EntityOps.kill(plugin, sender, type, world);
        return 1;
    }

    /**
     * Resolves the target world: the named world if given, otherwise a player
     * sender's current world. Messages and returns {@code null} on failure.
     *
     * @param sender    the command sender (for error messages)
     * @param worldName the requested world name, or {@code null} to use the sender's world
     * @return the resolved {@link World}, or {@code null} if it could not be resolved
     */
    private World resolveWorld(CommandSender sender, String worldName) {
        if (worldName != null) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                Msg.err(sender, "No loaded world named \"" + worldName + "\".");
            }
            return world;
        }
        if (sender instanceof Player player) {
            return player.getWorld();
        }
        Msg.err(sender, "Console must specify a world.");
        return null;
    }

    /** Tab-completes loaded world names, filtered by the typed prefix. */
    private CompletableFuture<Suggestions> worldSuggestions(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();
        for (World world : Bukkit.getWorlds()) {
            if (world.getName().toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(world.getName());
            }
        }
        return builder.buildFuture();
    }
}
