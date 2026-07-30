package dev.iyanz.sessentials.module.playerlist;

import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Online-player list GUI module (clean-room, CMI-style purpose — own layout). Registers
 * {@code /players} (alias {@code /plist}, permission {@code sessentials.playerlist}), which
 * opens a paged chest GUI of every online player as a textured head.
 *
 * <p>The GUI complements the {@code /manage} player-management GUI (module {@code managegui}):
 * clicking a head opens {@code /manage <player>} for viewers who hold {@code sessentials.manage},
 * and otherwise sends a teleport request via {@code /tpa <player>}. Both effects are produced by
 * <em>dispatching the existing, already-audited SEssentials command</em> rather than
 * re-implementing any logic, which keeps every effect Folia-safe by construction.</p>
 *
 * <p>Folia-safe: the GUI is a SELIB {@link PlayerListMenu} (opened on the viewer's region thread,
 * clicks routed there by the globally-registered SELIB menu listener); each head's live lore is
 * read on that target's own region thread and applied back on the viewer's region thread. No use
 * of the legacy {@code Bukkit.getScheduler()}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class PlayerListModule implements EssModule {

    @Override
    public String name() {
        return "playerlist";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("players")
                        .requires(s -> s.getSender().hasPermission("sessentials.playerlist"))
                        .executes(Cmds.playerExec(viewer -> new PlayerListMenu(plugin, viewer).open()))
                        .build(),
                "Open a paged GUI list of online players",
                List.of("plist")));
    }
}
