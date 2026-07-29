package dev.iyanz.sessentials.module.placeholders;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Bridges SEssentials data to <a href="https://www.spigotmc.org/resources/6245/">PlaceholderAPI</a>
 * by registering the {@code %sessentials_*%} expansion (see {@link SEssentialsExpansion}) when
 * PlaceholderAPI is present on the server.
 *
 * <p>PlaceholderAPI is only a soft/optional dependency: if it is not installed, {@link #enable}
 * logs an informational message and does nothing else, so the rest of the plugin is unaffected.</p>
 */
public final class PlaceholdersModule implements EssModule {

    @Override
    public String name() {
        return "placeholders";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            plugin.getLogger().info("PlaceholderAPI not found; skipping the sessentials placeholder expansion.");
            return;
        }
        new SEssentialsExpansion(plugin).register();
    }
}
