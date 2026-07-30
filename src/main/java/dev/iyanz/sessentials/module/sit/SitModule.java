package dev.iyanz.sessentials.module.sit;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Seating and riding module: {@code /sit} seats the player on an invisible marker
 * armor stand where they stand, {@code /ride} mounts the player on the entity they
 * are looking at, and {@code /shakeitoff} ejects everyone riding the player.
 *
 * <p>Seat entities are owned by {@link SitSeats} and removed again by
 * {@link SitListener} the moment the player dismounts or disconnects.</p>
 *
 * <p>Folia-safe: every entity spawn, mount, and removal runs on the owning entity's
 * region thread. The command executors themselves already run on the sender's region
 * thread, so self-only mutations ({@code /shakeitoff}) act directly; work on another
 * entity ({@code /ride}) hops to that entity's scheduler first.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class SitModule implements EssModule {

    /** Maximum ray-trace distance for {@code /ride}, in blocks. */
    private static final int RIDE_RANGE = 5;

    /** Owns the active seat entities and their cleanup. */
    private final SitSeats seats = new SitSeats();

    @Override
    public String name() {
        return "sit";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new SitListener(plugin, seats), plugin);
        plugin.commands(reg -> {
            reg.register(Commands.literal("sit")
                    .requires(s -> s.getSender().hasPermission("sessentials.sit"))
                    .executes(Cmds.playerExec(p -> seats.seat(plugin, p)))
                    .build(), "Sit down right where you are standing");
            reg.register(Commands.literal("ride")
                    .requires(s -> s.getSender().hasPermission("sessentials.ride"))
                    .executes(Cmds.playerExec(p -> ride(plugin, p)))
                    .build(), "Ride the entity you are looking at");
            reg.register(Commands.literal("shakeitoff")
                    .requires(s -> s.getSender().hasPermission("sessentials.shakeitoff"))
                    .executes(Cmds.playerExec(SitModule::shakeOff))
                    .build(), "Eject everyone riding you");
        });
    }

    @Override
    public void disable(SEssentialsPlugin plugin) {
        seats.clear();
    }

    /**
     * Mounts {@code player} on the entity they are looking at, if any is within
     * {@value #RIDE_RANGE} blocks. The mount itself runs on the target entity's region
     * thread, re-validating both entities first.
     *
     * @param plugin the owning plugin, used to hop to the target's region
     * @param player the player who wants to ride
     */
    private static void ride(SEssentialsPlugin plugin, Player player) {
        Entity target = player.getTargetEntity(RIDE_RANGE);
        if (target == null) {
            Msg.err(player, "No entity in sight within " + RIDE_RANGE + " blocks.");
            return;
        }
        Schedulers.entity(plugin, target, () -> {
            if (!target.isValid() || !player.isValid()) {
                return;
            }
            if (target.addPassenger(player)) {
                Msg.ok(player, "You are now riding " + target.getName() + ".");
            } else {
                Msg.err(player, "You cannot ride that.");
            }
        });
    }

    /**
     * Ejects every passenger currently riding {@code player}. The Brigadier executor
     * already runs on the sender's own region thread, so the mutation is safe here.
     *
     * @param player the player shaking off their riders
     */
    private static void shakeOff(Player player) {
        int riders = player.getPassengers().size();
        if (riders == 0) {
            Msg.info(player, "No one is riding you.");
            return;
        }
        player.eject();
        Msg.ok(player, riders == 1 ? "Shook off your rider." : "Shook off " + riders + " riders.");
    }
}
