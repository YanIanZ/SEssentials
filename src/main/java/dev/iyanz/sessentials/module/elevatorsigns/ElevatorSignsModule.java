package dev.iyanz.sessentials.module.elevatorsigns;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Elevator signs (CMI-style): right-clicking a sign whose first line is an elevator
 * marker — {@code [Up]}, {@code [Down]}, {@code [Lift Up]}, {@code [Lift Down]}
 * (case-insensitive, brackets optional) — teleports the player to the next elevator
 * sign above or below in the same block column.
 *
 * <p>Config ({@code config.yml}): {@code elevator.enabled} (default {@code true})
 * toggles the feature; {@code elevator.range} (default {@code 64}) caps how many
 * blocks up/down the destination search goes.</p>
 *
 * <p>Folia-safe: the interact event already runs on the clicking player's region
 * thread, block reads stay within that column, and the jump itself uses
 * {@link org.bukkit.entity.Player#teleportAsync(org.bukkit.Location)}.</p>
 */
public final class ElevatorSignsModule implements EssModule {

    @Override
    public String name() {
        return "elevatorsigns";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new ElevatorSignListener(plugin), plugin);
    }
}
