package dev.iyanz.sessentials.module.worldbehavior;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.iyanz.sessentials.SEssentialsPlugin;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Immutable snapshot of the {@code worldbehavior} module's configuration, read once when
 * the module enables. Grouping the parsing here keeps the listeners free of config
 * plumbing and lets the module decide up front which listeners are worth registering.
 *
 * <p>Three independent, config-gated behaviours are described:</p>
 * <ul>
 *   <li><b>Per-world forced game mode</b> — a map of world name → {@link GameMode} read
 *       from {@code world-gamemode.&lt;world&gt;}. World names are matched
 *       case-insensitively; unparseable modes are logged and skipped.</li>
 *   <li><b>Teleport to world spawn on world change</b> — the boolean
 *       {@code world-behavior.spawn-on-change}.</li>
 *   <li><b>Custom respawn</b> — {@code respawn.at-spawn} (send respawns to the global
 *       spawn) and {@code respawn.commands} (console commands to run on respawn).</li>
 * </ul>
 */
final class WorldBehaviorSettings {

    /** Config path of the world → game-mode map. */
    private static final String WORLD_GAMEMODE_SECTION = "world-gamemode";

    /** Config path of the "teleport to world spawn on change" toggle. */
    private static final String SPAWN_ON_CHANGE = "world-behavior.spawn-on-change";

    /** Config path of the "respawn at global spawn" toggle. */
    private static final String RESPAWN_AT_SPAWN = "respawn.at-spawn";

    /** Config path of the respawn console-command list. */
    private static final String RESPAWN_COMMANDS = "respawn.commands";

    /** World name (lower-cased) → forced game mode. */
    private final Map<String, GameMode> worldGameModes;
    private final boolean spawnOnChange;
    private final boolean respawnAtSpawn;
    private final List<String> respawnCommands;

    /**
     * Reads the module configuration once.
     *
     * @param plugin the owning plugin, used for config access and warning logs
     */
    WorldBehaviorSettings(SEssentialsPlugin plugin) {
        this.worldGameModes = readWorldGameModes(plugin);
        this.spawnOnChange = plugin.getConfig().getBoolean(SPAWN_ON_CHANGE, false);
        this.respawnAtSpawn = plugin.getConfig().getBoolean(RESPAWN_AT_SPAWN, false);
        this.respawnCommands = List.copyOf(plugin.getConfig().getStringList(RESPAWN_COMMANDS));
    }

    /**
     * Parses the {@code world-gamemode} section into a case-insensitive lookup table.
     * Each value is resolved to a {@link GameMode} by name (case-insensitive); entries
     * whose value is blank or names no known mode are logged and dropped so a typo can
     * never silently force the wrong mode.
     *
     * @param plugin the owning plugin
     * @return an immutable map of lower-cased world name → game mode (possibly empty)
     */
    private static Map<String, GameMode> readWorldGameModes(SEssentialsPlugin plugin) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(WORLD_GAMEMODE_SECTION);
        if (section == null) {
            return Map.of();
        }
        Map<String, GameMode> parsed = new java.util.HashMap<>();
        for (String world : section.getKeys(false)) {
            String raw = section.getString(world);
            GameMode mode = parseGameMode(raw);
            if (mode == null) {
                plugin.getLogger().warning("worldbehavior: ignoring invalid game mode '" + raw
                        + "' for world '" + world + "' (expected SURVIVAL, CREATIVE, ADVENTURE or SPECTATOR).");
                continue;
            }
            parsed.put(world.toLowerCase(Locale.ROOT), mode);
        }
        return Map.copyOf(parsed);
    }

    /**
     * Resolves a {@link GameMode} from its name, case-insensitively.
     *
     * @param input the configured mode name (may be {@code null})
     * @return the matching game mode, or {@code null} if none matches
     */
    private static GameMode parseGameMode(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        for (GameMode mode : GameMode.values()) {
            if (mode.name().equalsIgnoreCase(input.trim())) {
                return mode;
            }
        }
        return null;
    }

    /**
     * @param worldName the world's {@link org.bukkit.World#getName() name}
     * @return the forced game mode configured for that world, or {@code null} if none
     */
    GameMode forcedGameMode(String worldName) {
        return worldGameModes.get(worldName.toLowerCase(Locale.ROOT));
    }

    /** @return whether any per-world forced game mode is configured. */
    boolean hasForcedGameModes() {
        return !worldGameModes.isEmpty();
    }

    /** @return whether players should be sent to the new world's spawn on world change. */
    boolean spawnOnChange() {
        return spawnOnChange;
    }

    /** @return whether respawns should be redirected to the global spawn. */
    boolean respawnAtSpawn() {
        return respawnAtSpawn;
    }

    /** @return the console commands to run on respawn (a defensive copy, never {@code null}). */
    List<String> respawnCommands() {
        return respawnCommands;
    }

    /** @return whether any world-transition behaviour (forced mode or teleport) is active. */
    boolean hasWorldTransitionBehaviour() {
        return hasForcedGameModes() || spawnOnChange;
    }

    /** @return whether any respawn behaviour (redirect or commands) is active. */
    boolean hasRespawnBehaviour() {
        return respawnAtSpawn || !respawnCommands.isEmpty();
    }
}
