package dev.iyanz.sessentials.module.sit;

import dev.iyanz.sessentials.SEssentialsPlugin;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Seat entity lifecycle events for the sit module.
 *
 * <p>Seats players who right-click a bottom-half stair or slab with an empty main
 * hand (CMI-style click-to-sit, gated on the {@code sit.click-to-sit} config key),
 * and cleans up seat entities the moment they stop being used: when the seated
 * player dismounts (sneak, teleport, the seat being killed, or mounting something
 * else) and, as a safety net, when the player disconnects.</p>
 *
 * <p>All three events fire on the region thread that owns the player. Click-to-sit
 * spawns and mounts directly on that thread (the clicked block is within interaction
 * range, so it is owned by the same region), and seat removal is delegated to
 * {@link SitSeats}, which hops to the seat entity's own region thread — so no
 * thread-unsafe entity access happens here.</p>
 */
final class SitListener implements Listener {

    private final SEssentialsPlugin plugin;
    private final SitSeats seats;

    /**
     * @param plugin the owning plugin, passed through for region-thread scheduling
     * @param seats  the shared seat registry to clean up
     */
    SitListener(SEssentialsPlugin plugin, SitSeats seats) {
        this.plugin = plugin;
        this.seats = seats;
    }

    /**
     * Seats a player who right-clicks a sittable block: a bottom-half stair or bottom
     * slab, clicked with an empty main hand while not sneaking. Skipped when the
     * feature is disabled in config, the player lacks the {@code sessentials.sit}
     * permission, is already seated / in a vehicle / flying, or the block above the
     * seat is not passable (the seated player would suffocate). The seat itself is
     * spawned by {@link SitSeats#seatOnBlock(Player, Block)} into the same registry
     * as {@code /sit}, so dismount and quit cleanup apply unchanged.
     *
     * @param event the interact event, fired on the player's region thread
     */
    @EventHandler(ignoreCancelled = true)
    public void onClickToSit(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !isSittable(block) || event.getItem() != null) {
            return;
        }
        if (!plugin.getConfig().getBoolean("sit.click-to-sit", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (player.isSneaking() || player.isInsideVehicle() || player.isFlying()
                || seats.isSeated(player.getUniqueId())
                || !player.hasPermission("sessentials.sit")) {
            return;
        }
        if (!block.getRelative(BlockFace.UP).isPassable()) {
            return;
        }
        seats.seatOnBlock(player, block);
    }

    /**
     * @param block the clicked block
     * @return whether the block is a seatable shape: a bottom-half (non-upside-down)
     *         stair or a bottom slab, whose surface sits at half height
     */
    private static boolean isSittable(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Stairs stairs) {
            return stairs.getHalf() == Bisected.Half.BOTTOM;
        }
        if (data instanceof Slab slab) {
            return slab.getType() == Slab.Type.BOTTOM;
        }
        return false;
    }

    /**
     * Removes the seat once its player dismounts. Runs at {@code MONITOR} priority and
     * ignores cancelled events so a dismount vetoed by another plugin keeps the seat.
     *
     * @param event the dismount event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player) {
            seats.handleDismount(plugin, player, event.getDismounted());
        }
    }

    /**
     * Sweeps the quitting player's seat so no orphaned stand or map entry lingers.
     *
     * @param event the quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        seats.handleQuit(plugin, event.getPlayer().getUniqueId());
    }
}
