package dev.iyanz.sessentials.module.info;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

/**
 * Server-information commands: {@code /rules} and {@code /motd}, both driven by
 * configurable line lists in {@code config.yml} ({@code info.rules}, {@code info.motd})
 * with sensible built-in defaults when unset. The message of the day is also shown to
 * every player on join.
 *
 * <p>Unlike most commands in this plugin (which fall back to op-only when their
 * permission node isn't otherwise registered), {@code sessentials.rules} and
 * {@code sessentials.motd} are meant for every player, so this module registers them
 * at runtime with a {@code true} default instead of relying on that fallback.</p>
 */
public final class InfoModule implements EssModule {

    @Override
    public String name() {
        return "info";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        grantToEveryone(plugin, "sessentials.rules");
        grantToEveryone(plugin, "sessentials.motd");

        RulesCommand.register(plugin);
        MotdCommand.register(plugin);
        plugin.getServer().getPluginManager().registerEvents(new MotdListener(plugin), plugin);
    }

    /**
     * Registers {@code node} as a permission defaulting to {@code true} (granted to
     * everyone, including non-op players), unless it is already registered — e.g. by a
     * permissions plugin or a previous {@code /reload}.
     *
     * @param plugin the owning plugin
     * @param node   the permission node to register
     */
    private static void grantToEveryone(SEssentialsPlugin plugin, String node) {
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        if (pluginManager.getPermission(node) == null) {
            pluginManager.addPermission(new Permission(node, PermissionDefault.TRUE));
        }
    }
}
