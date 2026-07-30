package dev.iyanz.sessentials.module.joinbehavior;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Passive automatic behaviours applied on <b>every</b> player join and quit.
 *
 * <p>Unlike the {@code firstjoin} module (which runs only on a player's very first
 * connection), everything here fires on each join/quit and is entirely passive: no
 * commands are registered, only listeners. All behaviours are individually gated by
 * the {@code join} config section and default to <i>off</i>:</p>
 * <pre>
 * join:
 *   teleport-spawn: false            # teleport every joining player to spawn
 *   sound-enabled: false             # play join/quit sounds to everyone online
 *   join-sound: "entity.player.levelup"
 *   quit-sound: "block.note_block.bass"
 * </pre>
 *
 * <p>The spawn location is read (read-only) from the shared {@code spawn} data store
 * maintained by {@code /setspawn}; when no spawn has been set, the joining player's
 * world spawn is used as the fallback. Sound keys are resolved once at enable time
 * against {@link org.bukkit.Registry#SOUNDS}.</p>
 *
 * <p>Folia-safe: the join teleport uses {@code teleportAsync} directly from the join
 * event (already on the player's region thread), and each sound recipient is reached
 * on their own region thread. If no behaviour is enabled, no listener is registered
 * and the module is completely inert.</p>
 */
public final class JoinBehaviorModule implements EssModule {

    @Override
    public String name() {
        return "joinbehavior";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        boolean teleportSpawn = plugin.getConfig().getBoolean("join.teleport-spawn", false);
        boolean soundEnabled = plugin.getConfig().getBoolean("join.sound-enabled", false);
        if (!teleportSpawn && !soundEnabled) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(new JoinBehaviorListener(plugin), plugin);
    }
}
