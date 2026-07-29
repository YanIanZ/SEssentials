package dev.iyanz.sessentials.module.backpack;

import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.store.YamlStore;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Backpack module: a persistent, virtual extra inventory each player carries with them.
 *
 * <p>Opened with {@code /backpack} (aliases {@code /bp} and {@code /pv}), it is a fully
 * editable chest whose contents are saved to the {@code backpack} store when the GUI is
 * closed and restored on next open. Backpack size scales with the player's permissions:
 * {@code sessentials.backpack} grants the default 27 slots, while
 * {@code sessentials.backpack.<n>} (e.g. {@code .54} or {@code .9}) overrides it — the
 * largest held size wins.</p>
 *
 * @see BackpackMenu
 * @see BackpackCloseListener
 */
@SuppressWarnings("UnstableApiUsage")
public final class BackpackModule implements EssModule {

    @Override
    public String name() {
        return "backpack";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        YamlStore store = plugin.stores().get("backpack");
        plugin.getServer().getPluginManager().registerEvents(new BackpackCloseListener(store), plugin);

        plugin.commands(reg -> reg.register(Commands.literal("backpack")
                .requires(s -> s.getSender().hasPermission("sessentials.backpack"))
                .executes(Cmds.playerExec(player -> BackpackMenu.open(plugin, player, store)))
                .build(), "Open your personal backpack", List.of("bp", "pv")));
    }
}
