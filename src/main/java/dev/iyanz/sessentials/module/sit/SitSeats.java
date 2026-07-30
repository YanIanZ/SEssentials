package dev.iyanz.sessentials.module.sit;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Owns the invisible seat entities created by {@code /sit} and by right-clicking a
 * stair or slab (click-to-sit), and their lifecycle.
 *
 * <p>A seat is a marker {@link ArmorStand} (invisible, no gravity, no hitbox) spawned
 * just below the player's feet; the player is mounted on it as a passenger. Active
 * seats are tracked in a {@link ConcurrentHashMap} of seated player id to seat entity
 * id, and every entry is removed again on dismount or quit so a stale seat can never
 * linger.</p>
 *
 * <p>Folia-safe: the spawn/mount runs on the seated player's own region thread, and a
 * seat is always removed on the seat entity's owning region thread. Seat stands are
 * additionally marked non-persistent, so they are never written to disk and cannot
 * survive a restart even if the server stops while players are seated.</p>
 */
final class SitSeats {

    /** How far below the player's feet the seat marker spawns, in blocks. */
    private static final double SEAT_DROP = 0.2;

    /**
     * How far above a clicked block's base the seat marker spawns, in blocks. A
     * bottom-half stair or slab surface sits at +0.5; a player standing on it would
     * have the {@code /sit} stand at +0.5 &minus; {@link #SEAT_DROP} = +0.3, so this
     * value makes click-to-sit look identical to {@code /sit} on the same block.
     */
    private static final double BLOCK_SEAT_RISE = 0.3;

    /** Active seats: seated player id to seat armor-stand id. */
    private final ConcurrentHashMap<UUID, UUID> seats = new ConcurrentHashMap<>();

    /**
     * @param playerId the player to look up
     * @return whether that player currently has a tracked seat
     */
    boolean isSeated(UUID playerId) {
        return seats.containsKey(playerId);
    }

    /**
     * Seats {@code player} where they stand. Validates that the player is not already
     * seated, riding, or flying, then hops to the player's region thread to spawn the
     * seat and mount them.
     *
     * @param plugin the owning plugin, used to schedule onto the player's region
     * @param player the player to seat
     */
    void seat(SEssentialsPlugin plugin, Player player) {
        if (seats.containsKey(player.getUniqueId())) {
            Msg.err(player, "You are already sitting.");
            return;
        }
        if (player.isInsideVehicle()) {
            Msg.err(player, "You are already riding something.");
            return;
        }
        if (player.isFlying()) {
            Msg.err(player, "You cannot sit while flying.");
            return;
        }
        Schedulers.entity(plugin, player,
                () -> spawnSeat(player, player.getLocation().subtract(0.0, SEAT_DROP, 0.0)));
    }

    /**
     * Seats {@code player} centered on {@code block} (click-to-sit on a stair or slab).
     * Must be called on the player's own region thread — {@code PlayerInteractEvent}
     * already fires there, so the seat is spawned and mounted directly with no extra
     * scheduler hop. All guards are re-checked by {@link #spawnSeat(Player, Location)};
     * the resulting seat lives in the same registry as {@code /sit} seats, so the
     * existing dismount and quit cleanup applies unchanged.
     *
     * @param player the player to seat
     * @param block  the stair or slab block to sit on
     */
    void seatOnBlock(Player player, Block block) {
        spawnSeat(player, block.getLocation().add(0.5, BLOCK_SEAT_RISE, 0.5));
    }

    /**
     * Spawns the seat stand and mounts the player. Runs on the player's region thread;
     * re-checks the guards because the player may have moved on (quit, mounted
     * something, or issued {@code /sit} twice) between the command and this tick.
     *
     * @param player       the player to seat
     * @param seatLocation where the seat marker stand spawns
     */
    private void spawnSeat(Player player, Location seatLocation) {
        if (!player.isValid() || player.isInsideVehicle() || seats.containsKey(player.getUniqueId())) {
            return;
        }
        ArmorStand seat = player.getWorld().spawn(seatLocation, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setSmall(true);
            stand.setInvulnerable(true);
            stand.setPersistent(false);
        });
        if (!seat.addPassenger(player)) {
            seat.remove();
            Msg.err(player, "You cannot sit here.");
            return;
        }
        seats.put(player.getUniqueId(), seat.getUniqueId());
        Msg.ok(player, "You sit down. Sneak to stand up.");
    }

    /**
     * Handles a player dismounting from an entity. If the vehicle was this player's
     * tracked seat, the mapping is dropped and the seat entity is removed on its own
     * region thread (next tick, so the removal never re-enters the dismount event).
     *
     * @param plugin     the owning plugin, used to schedule onto the seat's region
     * @param player     the player who dismounted
     * @param dismounted the entity that was dismounted
     */
    void handleDismount(SEssentialsPlugin plugin, Player player, Entity dismounted) {
        if (seats.remove(player.getUniqueId(), dismounted.getUniqueId())) {
            Schedulers.entity(plugin, dismounted, dismounted::remove);
        }
    }

    /**
     * Drops the quitting player's seat, if any. Quitting while mounted also fires the
     * dismount event, so this is a belt-and-braces sweep: the mapping is always
     * cleared, and if the seat entity still exists it is removed on its region thread.
     *
     * @param plugin   the owning plugin, used to schedule onto the seat's region
     * @param playerId the id of the player who quit
     */
    void handleQuit(SEssentialsPlugin plugin, UUID playerId) {
        UUID seatId = seats.remove(playerId);
        if (seatId == null) {
            return;
        }
        Entity seat = Bukkit.getEntity(seatId);
        if (seat != null) {
            Schedulers.entity(plugin, seat, seat::remove);
        }
    }

    /**
     * Forgets all tracked seats on module shutdown. The stands themselves are
     * non-persistent, so any still in the world are discarded by the server on stop
     * and are never saved to disk.
     */
    void clear() {
        seats.clear();
    }
}
