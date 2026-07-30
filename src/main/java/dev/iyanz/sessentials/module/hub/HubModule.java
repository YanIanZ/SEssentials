package dev.iyanz.sessentials.module.hub;

import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Central menu-hub module (clean-room, CMI-style purpose — own layout). Registers
 * {@code /emenu} (alias {@code /hub}, permission {@code sessentials.hub}, player-only), which
 * opens a single chest GUI of themed icons linking to every other SEssentials sub-GUI.
 *
 * <p><b>Reuse, not re-implementation.</b> The hub owns no feature logic. Each icon simply
 * dispatches the viewer's own, already-registered command via {@link org.bukkit.entity.Player#performCommand(String)}
 * to open that sub-GUI (see {@link HubMenu}). An icon is shown only when its command is actually
 * registered on the server <em>and</em> the viewer holds the command's permission, so the hub
 * degrades gracefully if a module is disabled and never exposes an action the viewer cannot run.</p>
 *
 * <p>Folia-safe: the GUI is a SELIB {@link HubMenu} (opened on the viewer's region thread, clicks
 * routed there by the globally-registered SELIB menu listener); each icon's {@code performCommand}
 * runs on the viewer's region thread, exactly as if the viewer had typed the command, so the reused
 * sub-GUI opens from the correct thread. No use of the legacy {@code Bukkit.getScheduler()}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class HubModule implements EssModule {

    @Override
    public String name() {
        return "hub";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("emenu")
                        .requires(s -> s.getSender().hasPermission("sessentials.hub"))
                        .executes(Cmds.playerExec(viewer -> new HubMenu(plugin, viewer).open()))
                        .build(),
                "Open the central SEssentials menu hub",
                List.of("hub")));
    }
}
