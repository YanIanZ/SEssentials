package dev.iyanz.sessentials.module.disableenchant;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Blocks a configurable list of enchantments from being applied via the enchanting
 * table or the anvil (combining an item with an enchanted book).
 *
 * <p>Disabled by default ({@code disable-enchant.enabled}) so it never restricts a
 * server that hasn't opted in. Registration and config reads live in
 * {@link EnchantGateListener}; this class only wires the listener in.</p>
 */
public final class DisableEnchantModule implements EssModule {

    @Override
    public String name() {
        return "disableenchant";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new EnchantGateListener(plugin), plugin);
    }
}
