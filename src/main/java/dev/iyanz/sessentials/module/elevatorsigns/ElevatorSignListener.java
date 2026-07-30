package dev.iyanz.sessentials.module.elevatorsigns;

import java.util.Optional;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.util.Msg;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Listens for right-clicks on elevator signs and performs the vertical jump.
 *
 * <p>When the clicked sign's front line 0 is an elevator marker (see
 * {@link ElevatorDirection}), the listener scans the same X/Z column up or down —
 * at most {@code elevator.range} blocks (default {@value #DEFAULT_RANGE}) and never
 * past the world's build limits — for the next sign carrying any elevator marker,
 * then teleports the player onto it with
 * {@link Player#teleportAsync(Location)}, keeping the player's yaw and pitch.</p>
 *
 * <p>Folia notes: {@link PlayerInteractEvent} fires on the clicking player's region
 * thread; the scanned blocks share the clicked block's X/Z column (same region), so
 * reading them here is safe, and {@code teleportAsync} handles any cross-region
 * placement itself. No {@code Bukkit.getScheduler()} use.</p>
 */
final class ElevatorSignListener implements Listener {

    /** Fallback for {@code elevator.range}: how many blocks up/down to search. */
    private static final int DEFAULT_RANGE = 64;

    private final SEssentialsPlugin plugin;

    /** @param plugin the owning plugin, used to read the {@code elevator} config block */
    ElevatorSignListener(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles a right-click on a potential elevator sign.
     *
     * @param event the interaction; consumed (cancelled) only when an elevator marker matched
     */
    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign sign)) {
            return;
        }
        Optional<ElevatorDirection> direction = ElevatorDirection.parse(frontLine(sign));
        if (direction.isEmpty() || !plugin.getConfig().getBoolean("elevator.enabled", true)) {
            return;
        }
        event.setCancelled(true);

        Player player = event.getPlayer();
        Block destination = findDestination(block, direction.get());
        if (destination == null) {
            Msg.err(player, "No elevator sign that way.");
            return;
        }
        Location to = new Location(destination.getWorld(),
                destination.getX() + 0.5, destination.getY() + 1.0, destination.getZ() + 0.5,
                player.getLocation().getYaw(), player.getLocation().getPitch());
        player.teleportAsync(to);
    }

    /**
     * Scans the origin sign's block column for the next elevator sign in a direction.
     *
     * @param origin    the clicked elevator sign's block
     * @param direction which way to scan
     * @return the destination sign's block, or {@code null} if none within range
     */
    private Block findDestination(Block origin, ElevatorDirection direction) {
        World world = origin.getWorld();
        int range = Math.max(1, plugin.getConfig().getInt("elevator.range", DEFAULT_RANGE));
        for (int offset = 1; offset <= range; offset++) {
            int y = origin.getY() + offset * direction.step();
            if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
                break;
            }
            Block candidate = world.getBlockAt(origin.getX(), y, origin.getZ());
            if (!Tag.ALL_SIGNS.isTagged(candidate.getType())) {
                continue; // cheap material check before taking a block-state snapshot
            }
            if (candidate.getState() instanceof Sign candidateSign
                    && ElevatorDirection.isMarker(frontLine(candidateSign))) {
                return candidate;
            }
        }
        return null;
    }

    /** @return the plain text of {@code sign}'s front-side first line (never {@code null}). */
    private static String frontLine(Sign sign) {
        return PlainTextComponentSerializer.plainText().serialize(sign.getSide(Side.FRONT).line(0));
    }
}
