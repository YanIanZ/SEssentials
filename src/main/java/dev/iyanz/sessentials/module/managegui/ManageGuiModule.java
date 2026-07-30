package dev.iyanz.sessentials.module.managegui;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Player-management GUI module (CMI-style menu, clean-room). Registers {@code /manage <player>}
 * (permission {@code sessentials.manage}), which opens a chest GUI targeting the named online
 * player: a textured player-head with live info plus a grid of action buttons.
 *
 * <p>This module is a thin, reuse-only front-end. Every action button performs its effect by
 * <em>dispatching the corresponding, already-audited SEssentials command</em> rather than
 * re-implementing any logic (see {@link ManageMenu}): target-affecting actions are dispatched as
 * the console, viewer-context actions ({@code /tp}, {@code /invsee}, {@code /endersee}) as the
 * viewer. This keeps every effect Folia-safe by construction, because it runs exactly the same
 * command code path a staff member would type.</p>
 *
 * <p>Folia-safe: the GUI is a SELIB {@link ManageMenu} (opened on the viewer's region thread,
 * clicks routed there by the globally-registered SELIB menu listener); the live head refresh reads
 * the target on the target's region thread; console dispatches that touch world state are run on
 * the global region scheduler.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class ManageGuiModule implements EssModule {

    @Override
    public String name() {
        return "managegui";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("manage")
                .requires(s -> s.getSender().hasPermission("sessentials.manage"))
                .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                        .executes(ctx -> open(plugin, ctx)))
                .build(), "Open the player-management GUI for an online player"));
    }

    /** Resolves the viewer and the named online target, then opens the management menu. */
    private int open(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        Player viewer = Cmds.player(ctx);
        if (viewer == null) {
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Msg.err(viewer, name + " is not online.");
            return 0;
        }
        new ManageMenu(plugin, viewer, target).open();
        return 1;
    }
}
