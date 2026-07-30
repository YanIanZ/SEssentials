package dev.iyanz.sessentials.module.guiextras;

import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import io.papermc.paper.command.brigadier.Commands;

/**
 * GUI extras module: two clean-room chest menus in the CMI mould.
 *
 * <ul>
 *   <li>{@code /settings} (alias {@code /options}, permission {@code sessentials.settings}) — a
 *       personal panel of the viewer's own toggle commands ({@link SettingsMenu}). Each icon
 *       dispatches the viewer's existing toggle command, so the audited logic and per-command
 *       permissions are reused rather than duplicated.</li>
 *   <li>{@code /enchantgui} (permission {@code sessentials.enchantgui}) — a paginated grid of
 *       every registered enchantment ({@link EnchantMenu}); clicking one cycles its level on the
 *       item in the viewer's main hand.</li>
 * </ul>
 *
 * <p>Both commands are player-only (via {@link Cmds#playerExec}) and both menus route their clicks
 * through the SELIB menu base, so they are Folia-safe with no use of {@code Bukkit.getScheduler()}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class GuiExtrasModule implements EssModule {

    @Override
    public String name() {
        return "guiextras";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            reg.register(Commands.literal("settings")
                    .requires(s -> s.getSender().hasPermission("sessentials.settings"))
                    .executes(Cmds.playerExec(player -> SettingsMenu.open(plugin, player)))
                    .build(), "Open your personal settings toggles", List.of("options"));

            reg.register(Commands.literal("enchantgui")
                    .requires(s -> s.getSender().hasPermission("sessentials.enchantgui"))
                    .executes(Cmds.playerExec(player -> EnchantMenu.open(plugin, player)))
                    .build(), "Enchant the item in your hand from a menu");
        });
    }
}
