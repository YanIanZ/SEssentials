package dev.iyanz.sessentials.module.warpadmin;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

/**
 * Warp-admin GUI module (clean-room, CMI-style). Registers {@code /warpadmin}
 * (permission {@code sessentials.warpadmin}, player-only), which opens a paginated chest GUI
 * listing every warp; see {@link WarpAdminMenu}.
 *
 * <p>This module is a thin, reuse-only front-end. It reads the shared warp store read-only (via
 * {@link AdminWarps}) purely to render the list, and performs every mutation by dispatching the
 * existing, already-audited warp commands — teleport via {@code /warp}, delete via {@code /delwarp},
 * create via {@code /setwarp} — rather than re-implementing any warp logic or writing to the store
 * directly. It does not touch the warp module or any other module.</p>
 *
 * <p>Folia-safe by construction: the GUI is a SELIB menu (opened on the viewer's region thread,
 * clicks routed there by the globally-registered SELIB menu listener), and each dispatched command
 * runs on the same Folia thread a staff member typing it would use.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class WarpAdminModule implements EssModule {

    @Override
    public String name() {
        return "warpadmin";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(
                Commands.literal("warpadmin")
                        .requires(s -> s.getSender().hasPermission("sessentials.warpadmin"))
                        .executes(ctx -> {
                            Player viewer = Cmds.player(ctx);
                            if (viewer == null) {
                                return 0;
                            }
                            WarpAdminMenu.open(plugin, viewer);
                            return 1;
                        })
                        .build(),
                "Open the warp admin GUI"));
    }
}
