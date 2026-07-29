package dev.iyanz.sessentials.module.worlds;

import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

/**
 * Admin world management.
 *
 * <p>Commands: {@code /world} (bare, or {@code /worlds list}) lists loaded worlds;
 * {@code /world tp <world>} (alias {@code /worldtp}) teleports to a world's spawn;
 * {@code /world create <name> [environment] [seed]} creates and loads a new world;
 * {@code /world load <name>} loads an existing world folder; {@code /world unload
 * <name>} unloads a loaded world (never the default world); {@code /world setspawn}
 * sets the current world's spawn to your location; {@code /world gamerule <rule>
 * <value> [world]} sets a gamerule. {@code /worlds} is a full alias of {@code /world}.
 * Every subcommand requires {@code sessentials.worlds}. See {@link WorldOps} for the
 * Folia-safe implementation.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class WorldsModule implements EssModule {

    private static final String PERMISSION = "sessentials.worlds";

    @Override
    public String name() {
        return "worlds";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            reg.register(
                    Commands.literal("world")
                            .requires(s -> s.getSender().hasPermission(PERMISSION))
                            .executes(ctx -> {
                                WorldOps.list(plugin, ctx.getSource().getSender());
                                return 1;
                            })
                            .then(Commands.literal("list")
                                    .executes(ctx -> {
                                        WorldOps.list(plugin, ctx.getSource().getSender());
                                        return 1;
                                    }))
                            .then(Commands.literal("tp")
                                    .executes(ctx -> {
                                        Msg.err(ctx.getSource().getSender(), "Usage: /world tp <world>.");
                                        return 0;
                                    })
                                    .then(teleportArgument()))
                            .then(Commands.literal("create")
                                    .then(Commands.argument("name", StringArgumentType.word())
                                            .executes(ctx -> WorldOps.create(plugin, ctx.getSource().getSender(),
                                                    StringArgumentType.getString(ctx, "name"), null, null))
                                            .then(Commands.argument("environment", StringArgumentType.word())
                                                    .suggests(WorldSuggestions.ENVIRONMENTS)
                                                    .executes(ctx -> WorldOps.create(plugin, ctx.getSource().getSender(),
                                                            StringArgumentType.getString(ctx, "name"),
                                                            StringArgumentType.getString(ctx, "environment"), null))
                                                    .then(Commands.argument("seed", StringArgumentType.word())
                                                            .executes(ctx -> WorldOps.create(plugin,
                                                                    ctx.getSource().getSender(),
                                                                    StringArgumentType.getString(ctx, "name"),
                                                                    StringArgumentType.getString(ctx, "environment"),
                                                                    StringArgumentType.getString(ctx, "seed")))))))
                            .then(Commands.literal("load")
                                    .then(Commands.argument("name", StringArgumentType.word())
                                            .executes(ctx -> WorldOps.load(plugin, ctx.getSource().getSender(),
                                                    StringArgumentType.getString(ctx, "name")))))
                            .then(Commands.literal("unload")
                                    .then(Commands.argument("name", StringArgumentType.word())
                                            .suggests(WorldSuggestions.LOADED_WORLDS)
                                            .executes(ctx -> WorldOps.unload(plugin, ctx.getSource().getSender(),
                                                    StringArgumentType.getString(ctx, "name")))))
                            .then(Commands.literal("setspawn")
                                    .executes(ctx -> {
                                        Player p = Cmds.player(ctx);
                                        if (p == null) {
                                            return 0;
                                        }
                                        return WorldOps.setSpawn(plugin, p);
                                    }))
                            .then(Commands.literal("gamerule")
                                    .then(Commands.argument("rule", StringArgumentType.word())
                                            .suggests(WorldSuggestions.GAME_RULES)
                                            .then(Commands.argument("value", StringArgumentType.word())
                                                    .executes(ctx -> WorldOps.gameRule(plugin,
                                                            ctx.getSource().getSender(),
                                                            StringArgumentType.getString(ctx, "rule"),
                                                            StringArgumentType.getString(ctx, "value"), null))
                                                    .then(Commands.argument("world", StringArgumentType.word())
                                                            .suggests(WorldSuggestions.LOADED_WORLDS)
                                                            .executes(ctx -> WorldOps.gameRule(plugin,
                                                                    ctx.getSource().getSender(),
                                                                    StringArgumentType.getString(ctx, "rule"),
                                                                    StringArgumentType.getString(ctx, "value"),
                                                                    StringArgumentType.getString(ctx, "world")))))))
                            .build(),
                    "List, teleport to, create, load, unload and configure worlds",
                    List.of("worlds"));

            reg.register(
                    Commands.literal("worldtp")
                            .requires(s -> s.getSender().hasPermission(PERMISSION))
                            .executes(ctx -> {
                                Msg.err(ctx.getSource().getSender(), "Usage: /worldtp <world>.");
                                return 0;
                            })
                            .then(teleportArgument())
                            .build(),
                    "Teleport to a world's spawn point");
        });
    }

    /** Builds the {@code <world>} argument shared by {@code /world tp} and {@code /worldtp}. */
    private static RequiredArgumentBuilder<CommandSourceStack, String> teleportArgument() {
        return Commands.argument("world", StringArgumentType.word())
                .suggests(WorldSuggestions.LOADED_WORLDS)
                .executes(ctx -> {
                    Player p = Cmds.player(ctx);
                    if (p == null) {
                        return 0;
                    }
                    return WorldOps.teleport(p, StringArgumentType.getString(ctx, "world"));
                });
    }
}
