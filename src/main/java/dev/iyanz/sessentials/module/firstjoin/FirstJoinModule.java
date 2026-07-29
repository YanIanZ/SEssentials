package dev.iyanz.sessentials.module.firstjoin;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Runs one-time welcome actions the first time a player ever joins the server.
 *
 * <p>Driven entirely by config under the {@code first-join} section:</p>
 * <pre>
 * first-join:
 *   enabled: true
 *   teleport-to-spawn: true
 *   welcome: "&lt;gold&gt;Welcome &lt;yellow&gt;%player% &lt;gold&gt;to SourbyCraft!"
 *   commands: ["give %player% bread 16"]
 * </pre>
 *
 * <p>When enabled, this module registers a single {@link FirstJoinListener} that keys
 * off {@link org.bukkit.entity.Player#hasPlayedBefore()} to detect a genuine first
 * join, then (as configured) teleports the newcomer to spawn, broadcasts a welcome
 * message, and dispatches a list of console commands. If {@code first-join.enabled} is
 * {@code false} no listener is registered, so the feature is completely inert.</p>
 *
 * <p>Folia-safe: the teleport is scheduled on the player's own region thread, message
 * delivery hops to each recipient's region thread, and console commands run on the
 * global region scheduler.</p>
 */
public final class FirstJoinModule implements EssModule {

    @Override
    public String name() {
        return "firstjoin";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        if (!plugin.getConfig().getBoolean("first-join.enabled", true)) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(new FirstJoinListener(plugin), plugin);
    }
}
