package dev.iyanz.sessentials.module.itembrowser;

import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Item-browser GUI module (clean-room, CMI-style purpose — own layout). Registers
 * {@code /itemsgui} (alias {@code /igui}, permission {@code sessentials.itembrowser}), which
 * opens a paged chest GUI of every obtainable {@link org.bukkit.Material}, one icon each.
 *
 * <p>Clicking an icon hands the viewer that item: a left-click gives one, a shift-click gives a
 * full stack. Items go into the viewer's inventory and any overflow is dropped at their feet.</p>
 *
 * <p>Folia-safe: the GUI is a SELIB {@link ItemBrowserMenu} (opened on the viewer's region thread,
 * clicks routed there by the globally-registered SELIB menu listener), and the give action runs in
 * the click handler which already executes on the viewer's region thread. No use of the legacy
 * {@code Bukkit.getScheduler()}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class ItemBrowserModule implements EssModule {

    @Override
    public String name() {
        return "itembrowser";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("itemsgui")
                        .requires(s -> s.getSender().hasPermission("sessentials.itembrowser"))
                        .executes(Cmds.playerExec(viewer -> new ItemBrowserMenu(plugin, viewer).open()))
                        .build(),
                "Open a paged GUI browser of every obtainable item",
                List.of("igui")));
    }
}
