package dev.iyanz.sessentials.module.worldbehavior;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Passive, config-driven world and respawn behaviours in the style of CMI's per-world
 * automations. The module owns no commands — it only reacts to player events — and every
 * behaviour is off unless explicitly configured, so an empty config leaves it inert.
 *
 * <p>Configuration (all sections optional):</p>
 * <pre>
 * # Force a game mode per world (world name → SURVIVAL/CREATIVE/ADVENTURE/SPECTATOR).
 * world-gamemode:
 *   hub: ADVENTURE
 *   creative: CREATIVE
 *
 * world-behavior:
 *   # Teleport a player to the new world's spawn whenever they change worlds.
 *   spawn-on-change: false
 *
 * respawn:
 *   # Send respawns to the global /setspawn location (when one is set).
 *   at-spawn: false
 *   # Console commands run on respawn; {player} is substituted.
 *   commands: []
 * </pre>
 *
 * <p>The work is split across small helpers: {@link WorldBehaviorSettings} snapshots the
 * config, {@link WorldTransitionListener} handles per-world forced game mode plus the
 * teleport-on-change, and {@link RespawnListener} handles custom respawns. The spawn
 * location is read (never written) through {@link WorldSpawns}. Listeners are only
 * registered when their behaviour is actually configured.</p>
 *
 * <p>Folia-safe: entity mutations run on the owning player's region thread, all teleports
 * use {@code teleportAsync}, and console commands run on the global region scheduler.</p>
 */
public final class WorldBehaviorModule implements EssModule {

    @Override
    public String name() {
        return "worldbehavior";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        WorldBehaviorSettings settings = new WorldBehaviorSettings(plugin);

        if (settings.hasWorldTransitionBehaviour()) {
            plugin.getServer().getPluginManager()
                    .registerEvents(new WorldTransitionListener(plugin, settings), plugin);
        }
        if (settings.hasRespawnBehaviour()) {
            plugin.getServer().getPluginManager()
                    .registerEvents(new RespawnListener(plugin, settings), plugin);
        }
    }
}
