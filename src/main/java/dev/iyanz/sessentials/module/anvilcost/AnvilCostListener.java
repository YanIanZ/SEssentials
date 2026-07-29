package dev.iyanz.sessentials.module.anvilcost;

import dev.iyanz.sessentials.SEssentialsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;

/**
 * Caps the repair cost an anvil would otherwise charge, and lifts the vanilla
 * repair-cost ceiling so a capped (or free) repair is never blocked by the
 * "Too Expensive!" message.
 *
 * <p>Reads {@code anvil-cost.enabled} (default {@code false}) and {@code
 * anvil-cost.max-cost} (default {@code 0}, meaning free) fresh on every event, so a
 * config reload takes effect immediately. When {@code max-cost} is a positive number,
 * the cost is capped at {@code min(vanillaCost, max-cost)}; when it is {@code 0} or
 * negative, repairs are free. Either way {@link AnvilInventory#setMaximumRepairCost(int)}
 * is raised to an effectively unlimited value so the client never rejects the result as
 * too expensive while this module is enabled.</p>
 *
 * <p>A viewer holding {@code sessentials.anvilcost.bypass} is skipped entirely, keeping
 * the vanilla repair cost and cap for that player.</p>
 *
 * <p>{@link PrepareAnvilEvent} fires on the viewing player's own region thread, so this
 * listener needs no scheduler hop to touch the anvil inventory on Folia.</p>
 */
public final class AnvilCostListener implements Listener {

    /** Effectively unlimited, well under {@link Integer#MAX_VALUE} to avoid overflow. */
    private static final int UNLIMITED_MAX_REPAIR_COST = 2_000_000_000;

    private final SEssentialsPlugin plugin;

    /**
     * @param plugin the owning plugin, used for its live {@code anvil-cost.*} config
     */
    public AnvilCostListener(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Rewrites the anvil's repair cost (and its cap) for the previewing player. */
    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!plugin.getConfig().getBoolean("anvil-cost.enabled", false)) {
            return;
        }
        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }
        if (player.hasPermission("sessentials.anvilcost.bypass")) {
            return;
        }

        AnvilInventory inventory = event.getInventory();
        int maxCost = plugin.getConfig().getInt("anvil-cost.max-cost", 0);
        int cost = maxCost > 0 ? Math.min(inventory.getRepairCost(), maxCost) : 0;

        inventory.setRepairCost(cost);
        inventory.setMaximumRepairCost(UNLIMITED_MAX_REPAIR_COST);
    }
}
