package dev.iyanz.sessentials.module.itemframe;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Manage the item frame the sender is currently looking at.
 *
 * <p>Registers a single {@code /itemframe} command with four subcommands ({@code rotate},
 * {@code glow}, {@code visible}, {@code fixed}), each of which targets the {@link
 * org.bukkit.entity.ItemFrame} in the sender's crosshair and mutates it on that frame's
 * Folia region thread. The command logic lives in {@link ItemFrameCommands}.</p>
 */
public final class ItemFrameModule implements EssModule {

    @Override
    public String name() {
        return "itemframe";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        ItemFrameCommands.register(plugin);
    }
}
