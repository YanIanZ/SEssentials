package dev.iyanz.sessentials.api;

import dev.iyanz.sessentials.SEssentialsPlugin;

/**
 * A self-contained feature module. Each module registers its own commands (via the
 * Paper command lifecycle) and listeners in {@link #enable(SEssentialsPlugin)}, so
 * modules are independent and can be developed in isolation.
 *
 * <p>The plugin holds a list of modules and enables them all on startup; a module's
 * {@link #enable} runs after the core services (config, messages, economy, data
 * stores) are ready.</p>
 */
public interface EssModule {

    /** @return a short, unique module id (for logging). */
    String name();

    /**
     * Registers this module's commands and listeners.
     *
     * @param plugin the owning plugin (access to services)
     */
    void enable(SEssentialsPlugin plugin);

    /** Releases any resources on shutdown. Optional. */
    default void disable(SEssentialsPlugin plugin) {
    }
}
