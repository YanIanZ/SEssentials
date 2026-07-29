package dev.iyanz.sessentials.module.invtools;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Inventory maintenance and shulker-box tooling: opening a held shulker box as an
 * editable GUI, sorting the main inventory and merging same-item stacks.
 *
 * <p>See {@link InvToolsCommands} for the command implementations, {@link ShulkerEditMenu}
 * for the held-shulker editing GUI and {@link ShulkerCloseListener} for the write-back on
 * close.</p>
 */
public final class InvToolsModule implements EssModule {

    @Override
    public String name() {
        return "invtools";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new ShulkerCloseListener(), plugin);
        InvToolsCommands.register(plugin);
    }
}
