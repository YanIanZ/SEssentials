package dev.iyanz.sessentials.module.menus;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Adds a set of convenience GUI screens built on the SELIB menu framework, each opened by its
 * own player-only command and gated behind a per-command {@code sessentials.<cmd>} permission:
 *
 * <ul>
 *   <li>{@code /gmmenu} &rarr; {@link GameModeMenu} — switch your own game mode from a small
 *       chest GUI, showing only the modes you are permitted to use.</li>
 *   <li>{@code /ptimemenu} &rarr; {@link TimeWeatherMenu} — set your <em>personal</em>
 *       (client-side) time and weather; the menu stays open and re-renders in place.</li>
 *   <li>{@code /helpmenu} &rarr; {@link HelpMenu} — a paginated browser of command categories;
 *       clicking one lists that category's commands in chat.</li>
 * </ul>
 *
 * <p>Every screen extends a SELIB base ({@code Menu} / {@code PagedMenu}) and keeps all entity
 * mutations on the correct region thread, so the module is Folia-safe. The SELIB menu listener is
 * registered once by {@code Selib.init}, so this module only needs to register commands.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class MenusModule implements EssModule {

    @Override
    public String name() {
        return "menus";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            register(reg, "gmmenu", "Open the game-mode menu",
                    plugin, GameModeMenu::open);
            register(reg, "ptimemenu", "Open the personal time & weather menu",
                    plugin, TimeWeatherMenu::open);
            register(reg, "helpmenu", "Open the command help menu",
                    plugin, HelpMenu::open);
        });
    }

    /**
     * Registers a player-only command that opens a menu, gated behind {@code sessentials.<literal>}.
     *
     * @param reg         the Brigadier registrar
     * @param literal     the command name (also the permission suffix)
     * @param description the command help description
     * @param plugin      the owning plugin, passed to the menu opener
     * @param opener      opens the menu for a player
     */
    private static void register(Commands reg, String literal, String description,
                                 SEssentialsPlugin plugin, MenuOpener opener) {
        reg.register(
                Commands.literal(literal)
                        .requires(s -> s.getSender().hasPermission("sessentials." + literal))
                        .executes(Cmds.playerExec(player -> opener.open(plugin, player)))
                        .build(),
                description);
    }

    /** Opens a SELIB menu for a player (matches the {@code open(plugin, player)} menu factories). */
    @FunctionalInterface
    private interface MenuOpener {
        void open(SEssentialsPlugin plugin, org.bukkit.entity.Player player);
    }
}
