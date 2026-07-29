package dev.iyanz.sessentials.module.title;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * On-screen title module: {@code /title <player> <text>} shows a title to a single
 * online player, and {@code /titleall <text>} shows one to every online player.
 *
 * <p>The {@code <text>} argument is a greedy string; an optional subtitle is supplied
 * by splitting on the first {@code |} (left = title, right = subtitle, either may be
 * blank). Because both commands are gated behind operator permissions
 * ({@code sessentials.title} / {@code sessentials.titleall}), the text is rendered as
 * MiniMessage verbatim (colours, gradients, etc.), like the {@code /ctellraw} family.
 * See {@link TitleCommands} for details.</p>
 */
public final class TitleModule implements EssModule {

    @Override
    public String name() {
        return "title";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        TitleCommands.register(plugin);
    }
}
