package dev.iyanz.sessentials.module.chestlock;

import java.util.UUID;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Enforces container locks. A locked container may only be opened or broken by its
 * owner or a holder of {@code sessentials.chestlock.admin}; anyone else is blocked with
 * an error. When the owner (or an admin) breaks a locked container the lock entry is
 * cleared, so the freed location is not left reserved.
 *
 * <p>Both handlers run on the clicked/broken block's region thread — Folia fires
 * {@link PlayerInteractEvent} and {@link BlockBreakEvent} there already — so the block
 * reads below are region-safe without an explicit scheduler hop.</p>
 */
final class ChestLockListener implements Listener {

    private final SEssentialsPlugin plugin;

    /** @param plugin the owning plugin, used to reach the lock store */
    ChestLockListener(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Blocks a non-owner from opening a locked container via right-click. */
    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Container)) {
            return;
        }
        Player player = event.getPlayer();
        if (barred(player, block)) {
            event.setCancelled(true);
            Msg.err(player, "This container is locked.");
        }
    }

    /**
     * Blocks a non-owner from breaking a locked container; when the owner or an admin
     * breaks it, releases the lock so the location is no longer reserved.
     */
    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        UUID owner = ChestLocks.owner(plugin, block);
        if (owner == null) {
            return;
        }
        Player player = event.getPlayer();
        if (owner.equals(player.getUniqueId()) || player.hasPermission(ChestLockModule.PERM_ADMIN)) {
            ChestLocks.unlock(plugin, block);
            return;
        }
        event.setCancelled(true);
        Msg.err(player, "This container is locked.");
    }

    /** @return whether {@code player} is neither the owner of a locked {@code block} nor an admin. */
    private boolean barred(Player player, Block block) {
        UUID owner = ChestLocks.owner(plugin, block);
        if (owner == null) {
            return false;
        }
        return !owner.equals(player.getUniqueId()) && !player.hasPermission(ChestLockModule.PERM_ADMIN);
    }
}
