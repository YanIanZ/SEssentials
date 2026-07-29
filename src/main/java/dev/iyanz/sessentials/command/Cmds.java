package dev.iyanz.sessentials.command;

import java.util.Locale;
import java.util.function.Consumer;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

/**
 * Shared Brigadier helpers for command modules: online-player tab suggestions and a
 * player-only executor wrapper. Keeps every module's command code uniform.
 */
@ApiStatus.Experimental
@SuppressWarnings("UnstableApiUsage")
public final class Cmds {

    /** Tab-completes online player names, filtered by the typed prefix. */
    public static final SuggestionProvider<CommandSourceStack> PLAYERS = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(online.getName());
            }
        }
        return builder.buildFuture();
    };

    private Cmds() {
    }

    /** @return the sender as a {@link Player}, or {@code null} (after messaging) if it is the console. */
    public static Player player(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getSender() instanceof Player p) {
            return p;
        }
        Msg.err(ctx.getSource().getSender(), "Only players can use this.");
        return null;
    }

    /**
     * A Brigadier executor that runs {@code action} only for players.
     *
     * @param action the player action
     * @return a command returning 1 (handled) or 0 (console)
     */
    public static Command<CommandSourceStack> playerExec(Consumer<Player> action) {
        return ctx -> {
            Player p = player(ctx);
            if (p == null) {
                return 0;
            }
            action.accept(p);
            return 1;
        };
    }
}
