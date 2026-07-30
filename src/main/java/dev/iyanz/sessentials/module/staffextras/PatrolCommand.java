package dev.iyanz.sessentials.module.staffextras;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * {@code /patrol} ({@code sessentials.patrol}) — teleports the staff member to the
 * next online player in a stable name-ordered cycle, excluding themselves. Each staff
 * member keeps their own position in the cycle (in {@link StaffExtrasState}), so
 * repeated uses walk through everyone online.
 *
 * <p>Folia-safe: the target's location is read on the <em>target's</em> region
 * thread, the teleport is then issued back on the staff member's region thread via
 * {@link Player#teleportAsync(Location)}, and the outcome is reported from the
 * teleport future (messaging is thread-safe).</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class PatrolCommand {

    private PatrolCommand() {
    }

    /**
     * Registers {@code /patrol}.
     *
     * @param plugin the owning plugin
     * @param state  the shared state holding each staff member's cycle position
     */
    static void register(SEssentialsPlugin plugin, StaffExtrasState state) {
        plugin.commands(reg -> reg.register(Commands.literal("patrol")
                .requires(s -> s.getSender().hasPermission("sessentials.patrol"))
                .executes(Cmds.playerExec(staff -> patrol(plugin, state, staff)))
                .build(), "Teleport to the next online player on your patrol route"));
    }

    /** Picks the staff member's next patrol target and teleports them to it. */
    private static void patrol(SEssentialsPlugin plugin, StaffExtrasState state, Player staff) {
        List<Player> candidates = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(staff.getUniqueId())) {
                candidates.add(online);
            }
        }
        if (candidates.isEmpty()) {
            Msg.info(staff, "Nobody else is online to patrol.");
            return;
        }
        candidates.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));

        int index = state.nextPatrolIndex(staff.getUniqueId());
        Player target = candidates.get(Math.floorMod(index, candidates.size()));

        ScheduledTask hop = Schedulers.entity(plugin, target, () -> {
            Location location = target.getLocation();
            Schedulers.entity(plugin, staff, () -> staff.teleportAsync(location).thenAccept(done -> {
                if (done) {
                    Msg.ok(staff, "Patrolling: teleported to " + target.getName() + ".");
                } else {
                    Msg.err(staff, "Could not teleport to " + target.getName() + ".");
                }
            }));
        });
        if (hop == null) {
            Msg.err(staff, target.getName() + " just went offline; run /patrol again.");
        }
    }
}
