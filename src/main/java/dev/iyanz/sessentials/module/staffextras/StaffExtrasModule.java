package dev.iyanz.sessentials.module.staffextras;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Staff extras: covert and patrol tooling for moderators.
 *
 * <ul>
 *   <li>{@code /shadowmute <player>} — silent mute; the target still sees their own
 *       chat, nobody else does.</li>
 *   <li>{@code /search <item>} — count how many of a material every online player
 *       carries.</li>
 *   <li>{@code /patrol} — hop to the next online player in a per-staff cycle.</li>
 *   <li>{@code /notarget [player]} — make mobs ignore a player.</li>
 * </ul>
 *
 * <p>All state lives in a shared {@link StaffExtrasState} (concurrent collections,
 * cleaned on quit) and every cross-player action hops to the owning region thread,
 * keeping the module Folia-safe.</p>
 */
public final class StaffExtrasModule implements EssModule {

    /** Shared holder the commands write and the listener reads. */
    private final StaffExtrasState state = new StaffExtrasState();

    @Override
    public String name() {
        return "staffextras";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new StaffExtrasListener(state), plugin);
        ToggleCommands.register(plugin, state);
        SearchCommand.register(plugin);
        PatrolCommand.register(plugin, state);
    }
}
