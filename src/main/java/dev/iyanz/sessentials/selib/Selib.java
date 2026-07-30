package dev.iyanz.sessentials.selib;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.selib.gui.MenuListener;

/**
 * Facade and entry point for <b>SELIB</b> — SEssentials' internal GUI/effects library.
 * Registers the shared infrastructure (currently the SELIB menu {@link MenuListener}) exactly
 * once, so any module can build SELIB menus without wiring listeners itself.
 *
 * <p>Call {@link #init(SEssentialsPlugin)} from the plugin's {@code onEnable}. This coexists with
 * the legacy {@code dev.iyanz.sessentials.gui} package: the two menu listeners match different
 * holder types, so both can be registered during migration.</p>
 */
public final class Selib {

    /** Library name. */
    public static final String NAME = "SELIB";
    /** Library version. */
    public static final String VERSION = "1.0";

    private Selib() {
    }

    /**
     * Registers SELIB's shared listeners with the server. Call once from the plugin's
     * {@code onEnable}. No static "already initialized" guard: Bukkit unregisters a plugin's
     * listeners on disable via its {@link org.bukkit.event.HandlerList}, so a disable→enable
     * cycle (e.g. a plugin manager reload, same classloader) must re-register the SELIB menu
     * listener — a static guard would suppress that and leave every SELIB menu without click
     * routing after a reload.
     *
     * @param plugin the owning plugin
     */
    public static void init(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new MenuListener(), plugin);
    }
}
