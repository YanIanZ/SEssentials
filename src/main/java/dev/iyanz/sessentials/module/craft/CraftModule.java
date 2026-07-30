package dev.iyanz.sessentials.module.craft;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Craft module: {@code /craft} opens a virtual crafting table for the sender without
 * needing a placed workbench block.
 *
 * <p>Behaviour:</p>
 * <ul>
 *   <li>{@code /craft} — opens the vanilla 3x3 crafting grid for the sender
 *       (requires {@code sessentials.craft}).</li>
 * </ul>
 *
 * <p>The command executor already runs on the sender's Folia region thread, so the
 * inventory view can be opened directly with
 * {@link org.bukkit.entity.Player#openWorkbench(org.bukkit.Location, boolean)} using a
 * {@code null} location and {@code force = true} (no physical block required). That
 * method is deprecated in favour of the experimental {@code MenuType} API but remains
 * the stable, supported way to open a virtual workbench, so the deprecation is
 * suppressed deliberately.</p>
 */
@SuppressWarnings({"UnstableApiUsage", "deprecation"})
public final class CraftModule implements EssModule {

    @Override
    public String name() {
        return "craft";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("craft")
                .requires(s -> s.getSender().hasPermission("sessentials.craft"))
                .executes(Cmds.playerExec(player -> player.openWorkbench(null, true)))
                .build(), "Open a virtual crafting table"));
    }
}
