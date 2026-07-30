package dev.iyanz.sessentials.module.adminguis;

import java.util.UUID;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Admin GUI suite (clean-room, CMI-style). Registers two paginated chest GUIs, both
 * player-only:
 *
 * <ul>
 *   <li>{@code /worldgui} (permission {@code sessentials.worldgui}) — an environment-themed
 *       list of every loaded world; left-clicking a world teleports the viewer to it by
 *       reusing the existing, already-audited {@code /worldtp} command. See
 *       {@link WorldGuiMenu}.</li>
 *   <li>{@code /homeadmin <player>} (permission {@code sessentials.homeadmin}) — a paginated
 *       view of a target player's homes, read straight from the shared {@code home} data
 *       store; left-clicking teleports the viewer to a home, shift/right-clicking deletes it
 *       (with confirmation). Works for offline targets (resolved by UUID). See
 *       {@link HomeAdminMenu}.</li>
 * </ul>
 *
 * <p><strong>Reuse-only front-end.</strong> World teleports are delegated to {@code /worldtp}
 * rather than reimplemented, and world time/weather are never touched. Home data is read
 * read-only for rendering; the only mutation is deleting a home entry ({@code <uuid>.homes.<name>})
 * directly from the store, matching how the home module persists it.</p>
 *
 * <p>Folia-safe by construction: both menus are SELIB menus (opened on the viewer's region
 * thread, clicks routed there by the globally-registered SELIB menu listener), teleports use
 * {@link Player#teleportAsync teleportAsync}, and no {@code Bukkit.getScheduler()} is used.</p>
 */
@SuppressWarnings({"UnstableApiUsage", "deprecation"})
public final class AdminGuisModule implements EssModule {

    private static final String WORLD_GUI_PERMISSION = "sessentials.worldgui";
    private static final String HOME_ADMIN_PERMISSION = "sessentials.homeadmin";

    @Override
    public String name() {
        return "adminguis";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            reg.register(
                    Commands.literal("worldgui")
                            .requires(s -> s.getSender().hasPermission(WORLD_GUI_PERMISSION))
                            .executes(ctx -> {
                                Player viewer = Cmds.player(ctx);
                                if (viewer == null) {
                                    return 0;
                                }
                                WorldGuiMenu.open(plugin, viewer);
                                return 1;
                            })
                            .build(),
                    "Open the world teleport GUI");

            reg.register(
                    Commands.literal("homeadmin")
                            .requires(s -> s.getSender().hasPermission(HOME_ADMIN_PERMISSION))
                            .executes(ctx -> {
                                Msg.err(ctx.getSource().getSender(), "Usage: /homeadmin <player>.");
                                return 0;
                            })
                            .then(Commands.argument("player", StringArgumentType.word())
                                    .suggests(Cmds.PLAYERS)
                                    .executes(ctx -> {
                                        Player viewer = Cmds.player(ctx);
                                        if (viewer == null) {
                                            return 0;
                                        }
                                        String name = StringArgumentType.getString(ctx, "player");
                                        // getOfflinePlayer(String) is deprecated (it may resolve
                                        // names off-thread) but is the supported way to reach an
                                        // offline target; it yields the stable UUID the home store
                                        // is keyed by. No online/entity access is made on the target.
                                        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
                                        UUID uuid = target.getUniqueId();
                                        String display = target.getName() != null ? target.getName() : name;
                                        HomeAdminMenu.open(plugin, viewer, uuid, display);
                                        return 1;
                                    }))
                            .build(),
                    "Open the home admin GUI for a player");
        });
    }
}
