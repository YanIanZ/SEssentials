package dev.iyanz.sessentials.module.trade;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Player-to-player item trading. {@code /trade <player>} sends a 60-second request;
 * {@code /tradeaccept} opens a shared, live two-sided trade GUI for both players. Each
 * side places items in their own (left) half, sees a read-only mirror of the partner's
 * offer on the right, and toggles a confirm button; once <em>both</em> confirm, the
 * offered items are swapped into each other's inventories.
 *
 * <p>The whole flow is Folia-safe (every inventory op runs on the owning player's region
 * thread) and duplication-proof: offered items live solely in the menus and each menu is
 * drained exactly once, so every stack is delivered to precisely one destination. See
 * {@link TradeSession} for the full safety argument.</p>
 */
public final class TradeModule implements EssModule {

    @Override
    public String name() {
        return "trade";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        TradeManager manager = new TradeManager(plugin);
        plugin.getServer().getPluginManager().registerEvents(new TradeListener(manager), plugin);
        TradeCommands.register(plugin, manager);
    }
}
