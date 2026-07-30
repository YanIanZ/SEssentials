package dev.iyanz.sessentials.module.tpmenu;

import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Teleport-hub GUI module (clean-room, CMI-style purpose; own layout). Registers the player-only
 * command {@code /tpmenu} (permission {@code sessentials.tpmenu}, aliases {@code /tpgui} and
 * {@code /tphub}), which opens a SELIB chest menu of teleport shortcuts.
 *
 * <p><b>Reuse, not re-implementation.</b> Every button in the hub performs its effect by
 * <em>dispatching the viewer's existing SEssentials command</em> via
 * {@link org.bukkit.entity.Player#performCommand(String)} — reusing that command's already-audited,
 * Folia-safe logic and its own permission checks — rather than duplicating any teleport logic (see
 * {@link TpMenu} and {@link TpPlayersMenu}). A button is only shown when its underlying command is
 * actually registered on the server, so the hub degrades gracefully if a feature module is absent.</p>
 *
 * <p>Folia-safe: the GUI is a SELIB {@link TpMenu} opened on the viewer's region thread, with clicks
 * routed there by the globally-registered SELIB menu listener; each dispatched command therefore runs
 * on the viewer's own region thread, exactly as if they had typed it.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class TpMenuModule implements EssModule {

    @Override
    public String name() {
        return "tpmenu";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(
                Commands.literal("tpmenu")
                        .requires(s -> s.getSender().hasPermission("sessentials.tpmenu"))
                        .executes(Cmds.playerExec(player -> new TpMenu(plugin, player).open()))
                        .build(),
                "Open the teleport hub GUI",
                List.of("tpgui", "tphub")));
    }
}
