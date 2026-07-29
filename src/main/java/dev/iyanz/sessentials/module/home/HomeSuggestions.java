package dev.iyanz.sessentials.module.home;

import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.iyanz.sessentials.SEssentialsPlugin;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

/**
 * Tab-completes the command sender's own home names (for {@code /home} and
 * {@code /delhome}). Non-player senders (e.g. console) get no suggestions, since
 * homes are per-player.
 */
@ApiStatus.Experimental
@SuppressWarnings("UnstableApiUsage")
final class HomeSuggestions {

    private HomeSuggestions() {
    }

    /**
     * @param plugin the owning plugin (for home-store access)
     * @return a suggestion provider listing the sender's own home names, filtered by
     *         the currently typed prefix
     */
    static SuggestionProvider<CommandSourceStack> of(SEssentialsPlugin plugin) {
        return (ctx, builder) -> {
            if (ctx.getSource().getSender() instanceof Player player) {
                String remaining = builder.getRemainingLowerCase();
                List<String> names = Homes.names(plugin, player.getUniqueId());
                for (String name : names) {
                    if (name.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                        builder.suggest(name);
                    }
                }
            }
            return builder.buildFuture();
        };
    }
}
