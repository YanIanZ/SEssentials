package dev.iyanz.sessentials.module.chestlock;

import java.util.UUID;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;

/**
 * Simple owner-only container protection.
 *
 * <p>Commands: {@code /lock} records the container the sender is looking at (a
 * {@link Container} within {@value #REACH} blocks that is not already locked) as owned
 * by the sender; {@code /unlock} clears the lock on the container the sender is looking
 * at (owner, or a holder of {@code sessentials.chestlock.admin}). A companion
 * {@link ChestLockListener} then blocks non-owners from opening or breaking a locked
 * container. Both commands require {@code sessentials.chestlock}.</p>
 *
 * <p><b>Double chests (phase-1 limitation):</b> only the exact clicked half is locked,
 * so a double chest is fully protected only if both halves are locked individually.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class ChestLockModule implements EssModule {

    /** Base permission for {@code /lock} and {@code /unlock}. */
    static final String PERM = "sessentials.chestlock";

    /** Permission to unlock or bypass a container owned by someone else. */
    static final String PERM_ADMIN = "sessentials.chestlock.admin";

    /** Maximum look distance, in blocks, for targeting a container. */
    private static final int REACH = 6;

    @Override
    public String name() {
        return "chestlock";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new ChestLockListener(plugin), plugin);

        plugin.commands(reg -> {
            reg.register(
                    Commands.literal("lock")
                            .requires(s -> s.getSender().hasPermission(PERM))
                            .executes(Cmds.playerExec(p -> lock(plugin, p)))
                            .build(),
                    "Lock the container you are looking at");

            reg.register(
                    Commands.literal("unlock")
                            .requires(s -> s.getSender().hasPermission(PERM))
                            .executes(Cmds.playerExec(p -> unlock(plugin, p)))
                            .build(),
                    "Unlock the container you are looking at");
        });
    }

    /** Locks the container {@code player} is looking at to that player, if it is currently unlocked. */
    private static void lock(SEssentialsPlugin plugin, Player player) {
        Block block = targetContainer(player);
        if (block == null) {
            return;
        }
        if (ChestLocks.owner(plugin, block) != null) {
            Msg.err(player, "This container is already locked.");
            return;
        }
        ChestLocks.lock(plugin, block, player.getUniqueId());
        Msg.ok(player, "Container locked.");
    }

    /** Unlocks the container {@code player} is looking at, allowed for the owner or an admin. */
    private static void unlock(SEssentialsPlugin plugin, Player player) {
        Block block = targetContainer(player);
        if (block == null) {
            return;
        }
        UUID owner = ChestLocks.owner(plugin, block);
        if (owner == null) {
            Msg.err(player, "This container is not locked.");
            return;
        }
        if (!owner.equals(player.getUniqueId()) && !player.hasPermission(PERM_ADMIN)) {
            Msg.err(player, "You do not own this container.");
            return;
        }
        ChestLocks.unlock(plugin, block);
        Msg.ok(player, "Container unlocked.");
    }

    /**
     * @param player the looking player (already on their own region thread inside a command)
     * @return the {@link Container} block within {@value #REACH} blocks, or {@code null}
     *         after messaging the player that they must look at a container
     */
    private static Block targetContainer(Player player) {
        Block block = player.getTargetBlockExact(REACH);
        if (block == null || !(block.getState() instanceof Container)) {
            Msg.err(player, "You must be looking at a container.");
            return null;
        }
        return block;
    }
}
