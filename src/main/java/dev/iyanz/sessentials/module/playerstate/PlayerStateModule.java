package dev.iyanz.sessentials.module.playerstate;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Player-state admin commands: vitals ({@code /heal}, {@code /feed}), toggles
 * ({@code /god}, {@code /fly}, {@code /speed}), game mode switching
 * ({@code /gamemode}/{@code /gm} and the {@code /gmc}, {@code /gms}, {@code /gma},
 * {@code /gmsp} shortcuts), item utilities ({@code /hat}, {@code /repair}) and virtual
 * workstation openers ({@code /workbench}, {@code /anvil}, {@code /enderchest},
 * {@code /grindstone}, {@code /cartography}, {@code /loom}, {@code /smithingtable},
 * {@code /stonecutter}).
 *
 * <p>Every command that can target another player requires a {@code .others}
 * permission and, when acting on someone other than the sender, hops to that
 * player's region thread via {@link dev.iyanz.sessentials.scheduler.Schedulers}
 * before touching their entity state (Folia-safe).</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class PlayerStateModule implements EssModule {

    private final GodService godService = new GodService();

    @Override
    public String name() {
        return "playerstate";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new GodDamageListener(godService), plugin);
        plugin.getServer().getPluginManager().registerEvents(new GodQuitListener(godService), plugin);

        plugin.commands(reg -> {
            VitalityCommands.register(reg, plugin, godService);
            GameModeCommands.register(reg, plugin);
            ItemCommands.register(reg);
            WorkbenchCommands.register(reg, plugin);
        });
    }
}
