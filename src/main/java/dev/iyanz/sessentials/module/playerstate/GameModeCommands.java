package dev.iyanz.sessentials.module.playerstate;

import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Game mode commands: {@code /gamemode} (alias {@code /gm}), taking a mode by name
 * (case-insensitive) or numeric value 0-3, plus the one-shot shortcuts {@code /gmc},
 * {@code /gms}, {@code /gma} and {@code /gmsp}. All accept an optional
 * {@code [player]} target guarded by a {@code .others} permission.
 */
@SuppressWarnings("UnstableApiUsage")
final class GameModeCommands {

    private static final SuggestionProvider<CommandSourceStack> MODES = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (String option : List.of("survival", "creative", "adventure", "spectator", "0", "1", "2", "3")) {
            if (option.startsWith(remaining)) {
                builder.suggest(option);
            }
        }
        return builder.buildFuture();
    };

    private GameModeCommands() {
    }

    static void register(Commands reg, SEssentialsPlugin plugin) {
        reg.register(
                Commands.literal("gamemode")
                        .requires(s -> s.getSender().hasPermission("sessentials.gamemode"))
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests(MODES)
                                .executes(ctx -> {
                                    Player self = Cmds.player(ctx);
                                    if (self == null) {
                                        return 0;
                                    }
                                    return applyToSelf(ctx.getSource().getSender(), self,
                                            StringArgumentType.getString(ctx, "mode"));
                                })
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .suggests(Cmds.PLAYERS)
                                        .requires(s -> s.getSender().hasPermission("sessentials.gamemode.others"))
                                        .executes(ctx -> applyToTarget(ctx, plugin))))
                        .build(),
                "Change your (or a player's) game mode",
                List.of("gm"));

        registerShortcut(reg, plugin, "gmc", GameMode.CREATIVE);
        registerShortcut(reg, plugin, "gms", GameMode.SURVIVAL);
        registerShortcut(reg, plugin, "gma", GameMode.ADVENTURE);
        registerShortcut(reg, plugin, "gmsp", GameMode.SPECTATOR);
    }

    private static void registerShortcut(Commands reg, SEssentialsPlugin plugin, String literal, GameMode mode) {
        reg.register(
                CommandBuilders.selfOrTarget(literal, "sessentials." + literal, "sessentials." + literal + ".others",
                        plugin, (sender, player) -> setMode(sender, player, mode)),
                "Switch to " + mode.name().toLowerCase(Locale.ROOT) + " mode");
    }

    private static int applyToSelf(CommandSender sender, Player self, String modeInput) {
        GameMode mode = parseMode(modeInput);
        if (mode == null) {
            Msg.err(sender, "Unknown game mode: " + modeInput);
            return 0;
        }
        setMode(sender, self, mode);
        return 1;
    }

    private static int applyToTarget(CommandContext<CommandSourceStack> ctx, SEssentialsPlugin plugin) {
        CommandSender sender = ctx.getSource().getSender();
        GameMode mode = parseMode(StringArgumentType.getString(ctx, "mode"));
        if (mode == null) {
            Msg.err(sender, "Unknown game mode: " + StringArgumentType.getString(ctx, "mode"));
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "target");
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Msg.err(sender, name + " is not online.");
            return 0;
        }
        Schedulers.entity(plugin, target, () -> setMode(sender, target, mode));
        return 1;
    }

    private static void setMode(CommandSender sender, Player player, GameMode mode) {
        player.setGameMode(mode);
        String modeName = mode.name().toLowerCase(Locale.ROOT);
        Msg.ok(player, "Game mode set to " + modeName + ".");
        if (sender != player) {
            Msg.ok(sender, "Set " + player.getName() + "'s game mode to " + modeName + ".");
        }
    }

    /** Parses a game mode from its name (case-insensitive) or numeric value 0-3. */
    private static GameMode parseMode(String input) {
        try {
            return GameMode.getByValue(Integer.parseInt(input));
        } catch (NumberFormatException notNumeric) {
            for (GameMode mode : GameMode.values()) {
                if (mode.name().equalsIgnoreCase(input)) {
                    return mode;
                }
            }
            return null;
        }
    }
}
