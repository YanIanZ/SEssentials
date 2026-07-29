package dev.iyanz.sessentials.module.glow;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Glow module: {@code /glow [color] [player]} toggles a player's glowing outline and,
 * when a colour is supplied, tints it.
 *
 * <p>Behaviour:</p>
 * <ul>
 *   <li>{@code /glow} — toggles the sender's glow on or off (white when on).</li>
 *   <li>{@code /glow <color>} — makes the sender glow in {@code <color>}; the words
 *       {@code off}/{@code none} instead turn glowing off.</li>
 *   <li>{@code /glow <color> <player>} — the same, applied to another player
 *       (requires {@code sessentials.glow.others}).</li>
 * </ul>
 *
 * <p>The colour is realised through a per-colour scoreboard team on the main
 * scoreboard (see {@link GlowTeams}). Every entity/scoreboard mutation runs on the
 * affected player's Folia region thread: inline for the self-targeting cases (a command
 * executor already runs on the sender's region thread) and via
 * {@link Schedulers#entity} when acting on another player.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class GlowModule implements EssModule {

    /** Colour tokens that disable glowing instead of naming a colour. */
    private static final Set<String> OFF_TOKENS = Set.of("off", "none", "clear");

    /** Tab-completes the disable tokens and every named colour, filtered by prefix. */
    private static final SuggestionProvider<CommandSourceStack> COLORS = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (String suggestion : colorSuggestions()) {
            if (suggestion.startsWith(remaining)) {
                builder.suggest(suggestion);
            }
        }
        return builder.buildFuture();
    };

    @Override
    public String name() {
        return "glow";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("glow")
                .requires(s -> s.getSender().hasPermission("sessentials.glow"))
                .executes(Cmds.playerExec(GlowModule::toggleSelf))
                .then(Commands.argument("color", StringArgumentType.word())
                        .suggests(COLORS)
                        .executes(GlowModule::glowSelf)
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(Cmds.PLAYERS)
                                .requires(s -> s.getSender().hasPermission("sessentials.glow.others"))
                                .executes(ctx -> glowOther(plugin, ctx))))
                .build(), "Toggle your glow, optionally in a colour"));
    }

    /** Toggles the sender's glow (inline: already on the sender's region thread). */
    private static void toggleSelf(Player player) {
        if (player.isGlowing()) {
            disable(player);
            Msg.info(player, "You are no longer glowing.");
        } else {
            player.setGlowing(true);
            Msg.ok(player, "You are now glowing.");
        }
    }

    /** Applies a colour (or disables) for the sender, inline on their region thread. */
    private static int glowSelf(CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        String colorArg = StringArgumentType.getString(ctx, "color");
        return applyColor(player, colorArg, player) ? 1 : 0;
    }

    /** Applies a colour (or disables) for another player, hopped to their region thread. */
    private static int glowOther(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String colorArg = StringArgumentType.getString(ctx, "color");
        String targetName = StringArgumentType.getString(ctx, "player");

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            Msg.err(sender, targetName + " is not online.");
            return 0;
        }
        if (target.equals(sender)) {
            return applyColor(target, colorArg, target) ? 1 : 0;
        }

        NamedTextColor color = resolveOrReport(colorArg, sender);
        if (color == null && !isOff(colorArg)) {
            return 0;
        }
        Schedulers.entity(plugin, target, () -> {
            if (color == null) {
                disable(target);
            } else {
                GlowTeams.apply(target, color);
                target.setGlowing(true);
            }
            sendResult(target, colorArg, color);
        });
        Msg.ok(sender, feedbackFor(target.getName(), colorArg, color));
        return 1;
    }

    /**
     * Applies {@code colorArg} to {@code player} on the current (region) thread and
     * messages {@code player}.
     *
     * @return {@code true} if the argument was valid and applied, {@code false} otherwise
     */
    private static boolean applyColor(Player player, String colorArg, Player messageTo) {
        if (isOff(colorArg)) {
            disable(player);
            sendResult(player, colorArg, null);
            return true;
        }
        NamedTextColor color = resolveOrReport(colorArg, messageTo);
        if (color == null) {
            return false;
        }
        GlowTeams.apply(player, color);
        player.setGlowing(true);
        sendResult(player, colorArg, color);
        return true;
    }

    /** Turns glow off and detaches the player from any glow team. */
    private static void disable(Player player) {
        player.setGlowing(false);
        GlowTeams.clear(player);
    }

    /** @return the resolved colour, or {@code null} after messaging {@code to} on an unknown name. */
    private static NamedTextColor resolveOrReport(String colorArg, CommandSender to) {
        if (isOff(colorArg)) {
            return null;
        }
        NamedTextColor color = GlowTeams.color(colorArg);
        if (color == null) {
            Msg.err(to, "Unknown colour: " + colorArg + ".");
        }
        return color;
    }

    /** Messages the affected player about the outcome of a colour/disable action. */
    private static void sendResult(Player player, String colorArg, NamedTextColor color) {
        if (color == null) {
            Msg.info(player, "You are no longer glowing.");
        } else {
            Msg.ok(player, "You are now glowing " + NamedTextColor.NAMES.key(color) + ".");
        }
    }

    /** @return the sender-facing confirmation line for an action on {@code targetName}. */
    private static String feedbackFor(String targetName, String colorArg, NamedTextColor color) {
        if (color == null) {
            return "Stopped " + targetName + " glowing.";
        }
        return "Set " + targetName + "'s glow to " + NamedTextColor.NAMES.key(color) + ".";
    }

    /** @return {@code true} if the argument requests disabling rather than a colour. */
    private static boolean isOff(String colorArg) {
        return OFF_TOKENS.contains(colorArg.toLowerCase(Locale.ROOT));
    }

    /** @return the ordered suggestion list: disable tokens followed by every named colour. */
    private static List<String> colorSuggestions() {
        List<String> suggestions = new ArrayList<>(OFF_TOKENS);
        suggestions.addAll(NamedTextColor.NAMES.keys());
        return suggestions;
    }
}
