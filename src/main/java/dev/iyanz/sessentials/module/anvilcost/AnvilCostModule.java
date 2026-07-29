package dev.iyanz.sessentials.module.anvilcost;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Caps (or waives) the XP-level cost of anvil repairs and removes the vanilla
 * "Too Expensive!" ceiling while doing so.
 *
 * <p>Disabled by default ({@code anvil-cost.enabled}) so it never changes anvil
 * behaviour on a server that hasn't opted in. Registration and config reads live in
 * {@link AnvilCostListener}; this class only wires the listener in.</p>
 */
public final class AnvilCostModule implements EssModule {

    @Override
    public String name() {
        return "anvilcost";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new AnvilCostListener(plugin), plugin);
    }
}
