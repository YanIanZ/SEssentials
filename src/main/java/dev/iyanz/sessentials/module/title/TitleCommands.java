package dev.iyanz.sessentials.module.title;

import java.time.Duration;
import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Registers and handles the operator on-screen title commands: {@code /title}
 * (show a title to one online player) and {@code /titleall} (show a title to every
 * online player).
 *
 * <p>The {@code text} argument is a greedy string. An optional subtitle is supplied
 * by splitting {@code text} on the first {@code |}: the left side is the title and the
 * right side is the subtitle, and either side may be blank. Both parts are parsed as
 * MiniMessage verbatim — this is safe because both commands are gated behind operator
 * permissions, so the input is trusted staff-authored rich text, not player chat.</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class TitleCommands {

    /** Permission for {@code /title <player> <text>}. */
    private static final String PERMISSION_TITLE = "sessentials.title";

    /** Permission for {@code /titleall <text>}. */
    private static final String PERMISSION_TITLE_ALL = "sessentials.titleall";

    /** How long the title fades in before it is fully shown. */
    private static final Duration FADE_IN = Duration.ofMillis(500);

    /** How long the title stays fully shown. */
    private static final Duration STAY = Duration.ofSeconds(3);

    /** How long the title fades out after being shown. */
    private static final Duration FADE_OUT = Duration.ofMillis(500);

    private TitleCommands() {
    }

    /** Registers {@code /title} and {@code /titleall}. */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            reg.register(Commands.literal("title")
                    .requires(s -> s.getSender().hasPermission(PERMISSION_TITLE))
                    .then(Commands.argument("player", StringArgumentType.word())
                            .suggests(Cmds.PLAYERS)
                            .then(Commands.argument("text", StringArgumentType.greedyString())
                                    .executes(ctx -> handleTitle(plugin, ctx))))
                    .build(), "Show a title to a player", List.of());

            reg.register(Commands.literal("titleall")
                    .requires(s -> s.getSender().hasPermission(PERMISSION_TITLE_ALL))
                    .then(Commands.argument("text", StringArgumentType.greedyString())
                            .executes(ctx -> handleTitleAll(plugin, ctx)))
                    .build(), "Show a title to every online player", List.of());
        });
    }

    /**
     * Builds the title from the named online player and delivers it on that player's
     * own region thread (Folia-safe). Errors to the sender if the target is offline.
     */
    private static int handleTitle(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Msg.err(ctx.getSource().getSender(), name + " is not online.");
            return 0;
        }
        Title title = buildTitle(StringArgumentType.getString(ctx, "text"));
        Schedulers.entity(plugin, target, () -> target.showTitle(title));
        Msg.ok(ctx.getSource().getSender(), "Title shown to " + target.getName() + ".");
        return 1;
    }

    /**
     * Builds the title and delivers it to every online player, each on its own region
     * thread (Folia-safe).
     */
    private static int handleTitleAll(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        Title title = buildTitle(StringArgumentType.getString(ctx, "text"));
        for (Player player : Bukkit.getOnlinePlayers()) {
            Schedulers.entity(plugin, player, () -> player.showTitle(title));
        }
        Msg.ok(ctx.getSource().getSender(), "Title shown to everyone.");
        return 1;
    }

    /**
     * Parses the raw {@code text} into a title/subtitle pair and wraps it with the
     * standard fade/stay/fade timings. The text is split on the first {@code |}: the
     * left side is the title and the right side is the subtitle (either may be blank).
     * Both sides are rendered with MiniMessage.
     *
     * @param text the raw greedy-string argument
     * @return the assembled {@link Title}
     */
    private static Title buildTitle(String text) {
        int split = text.indexOf('|');
        String titlePart = split >= 0 ? text.substring(0, split) : text;
        String subPart = split >= 0 ? text.substring(split + 1) : "";
        Component main = Msg.mm(titlePart);
        Component sub = Msg.mm(subPart);
        return Title.title(main, sub, Title.Times.times(FADE_IN, STAY, FADE_OUT));
    }
}
