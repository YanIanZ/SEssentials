package dev.iyanz.sessentials.module.chat;

import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

/**
 * Registers {@code /chatcolor <color|gradient|off>}: sets, or clears, the SENDER's own
 * persisted chat colour. {@link ChatListener} applies the stored value to every
 * message the player sends afterwards.
 *
 * <p>The colour argument accepts a bare tag name (e.g. {@code red}), a hex code
 * ({@code #43c6ac}), a gradient ({@code gradient:#43C6AC:#4FC3F7}, stops may also be
 * named colours), any of those wrapped in {@code <...>}, or {@code off} to reset. It
 * is read as a single greedy argument so a gradient's {@code #}/{@code :} characters
 * never need quoting.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class ChatColorCommand {

    private static final List<String> SUGGESTIONS = List.of(
            "off", "red", "green", "aqua", "gold", "yellow", "blue", "light_purple",
            "gray", "dark_gray", "white", "black", "dark_red", "dark_green", "dark_aqua",
            "dark_blue", "dark_purple", "gradient:#FF5555:#5555FF");

    private static final SuggestionProvider<CommandSourceStack> COLORS = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (String suggestion : SUGGESTIONS) {
            if (suggestion.startsWith(remaining)) {
                builder.suggest(suggestion);
            }
        }
        return builder.buildFuture();
    };

    private ChatColorCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin       the owning plugin
     * @param colorService the service used to validate/persist chat colours
     */
    public static void register(SEssentialsPlugin plugin, ChatColorService colorService) {
        plugin.commands(reg -> {
            var node = Commands.literal("chatcolor")
                    .requires(s -> s.getSender().hasPermission("sessentials.chatcolor"))
                    .then(Commands.argument("value", StringArgumentType.greedyString())
                            .suggests(COLORS)
                            .executes(ctx -> {
                                Player p = Cmds.player(ctx);
                                if (p == null) {
                                    return 0;
                                }
                                String raw = StringArgumentType.getString(ctx, "value");
                                if (raw.equalsIgnoreCase("off")) {
                                    colorService.clear(p.getUniqueId());
                                    Msg.ok(p, "Chat colour reset.");
                                    return 1;
                                }
                                try {
                                    String tag = colorService.normalize(raw);
                                    colorService.set(p.getUniqueId(), tag);
                                    Msg.ok(p, "Chat colour set.");
                                    return 1;
                                } catch (IllegalArgumentException ex) {
                                    Msg.err(p, ex.getMessage());
                                    return 0;
                                }
                            }))
                    .build();
            reg.register(node, "Set your chat colour or gradient");
        });
    }
}
