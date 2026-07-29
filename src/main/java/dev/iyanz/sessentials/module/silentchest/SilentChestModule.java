package dev.iyanz.sessentials.module.silentchest;

import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Silent container peeking: {@code /silentchest} (aliases {@code /silentcontainer},
 * {@code /sc}) toggles a per-player mode in which right-clicking any
 * {@link org.bukkit.block.Container} block (chest, barrel, hopper, furnace, brewing
 * stand, ...) opens a read/write snapshot of its contents instead of the real
 * container, so nearby players never see the vanilla open animation or hear the
 * open sound.
 *
 * <p>No packet library is used: the trick is that the interaction which would
 * normally open the real block is cancelled outright (see
 * {@link SilentChestListener}), and a plain, unrelated {@link Inventory} copy is
 * shown to the viewer instead (see {@link SilentChestView}). Since the player is
 * never handed the real container's inventory, the block entity never registers a
 * viewer and so never animates or plays a sound. Edits made in the snapshot are
 * written back into the real block on close (see {@link SilentChestCloseListener}),
 * on that block's own region thread.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class SilentChestModule implements EssModule {

    private final SilentChestService service = new SilentChestService();

    @Override
    public String name() {
        return "silentchest";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new SilentChestListener(service), plugin);
        plugin.getServer().getPluginManager().registerEvents(new SilentChestCloseListener(plugin), plugin);

        plugin.commands(reg -> reg.register(
                Commands.literal("silentchest")
                        .requires(s -> s.getSender().hasPermission("sessentials.silentchest"))
                        .executes(Cmds.playerExec(this::toggle))
                        .build(),
                "Toggle silent container peeking",
                List.of("silentcontainer", "sc")));
    }

    /** Toggles silent-chest mode for {@code player} and confirms the new state to them. */
    private void toggle(Player player) {
        boolean nowSilent = service.toggle(player.getUniqueId());
        if (nowSilent) {
            Msg.ok(player, "Silent chest mode enabled. Containers you open won't alert nearby players.");
        } else {
            Msg.ok(player, "Silent chest mode disabled.");
        }
    }
}
