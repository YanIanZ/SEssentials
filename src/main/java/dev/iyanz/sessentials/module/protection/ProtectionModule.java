package dev.iyanz.sessentials.module.protection;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Passive protection module: automatic, config-gated safety behaviours that need no
 * commands or permissions.
 *
 * <p>Driven entirely by config under the {@code protection} section:</p>
 * <pre>
 * protection:
 *   void-teleport: true
 *   no-phantoms: false
 * </pre>
 *
 * <p>Behaviours (each independently toggleable):</p>
 * <ul>
 *   <li><b>Void rescue</b> ({@code protection.void-teleport}, default {@code true}) —
 *       cancels void damage to players and teleports them to the server spawn (the
 *       {@code spawn} data store location set by {@code /setspawn}, or the world's
 *       vanilla spawn when none is stored). See {@link VoidRescueListener}.</li>
 *   <li><b>Anti-phantom</b> ({@code protection.no-phantoms}, default {@code false}) —
 *       cancels naturally occurring phantom spawns so sleepless players are never
 *       harassed. See {@link PhantomBlockListener}.</li>
 * </ul>
 *
 * <p>Folia-safe: both listeners run on the region thread that fired the event and use
 * only region-safe API ({@code teleportAsync}, event cancellation); no schedulers are
 * involved. A disabled behaviour registers no listener at all, so it is completely
 * inert.</p>
 */
public final class ProtectionModule implements EssModule {

    @Override
    public String name() {
        return "protection";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        if (plugin.getConfig().getBoolean("protection.void-teleport", true)) {
            plugin.getServer().getPluginManager()
                    .registerEvents(new VoidRescueListener(plugin), plugin);
        }
        if (plugin.getConfig().getBoolean("protection.no-phantoms", false)) {
            plugin.getServer().getPluginManager()
                    .registerEvents(new PhantomBlockListener(), plugin);
        }
    }
}
