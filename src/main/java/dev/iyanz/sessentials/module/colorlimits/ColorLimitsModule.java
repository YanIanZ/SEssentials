package dev.iyanz.sessentials.module.colorlimits;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Strips colour/formatting from chat messages sent by players who lack the
 * {@code sessentials.chat.color} permission.
 *
 * <p>Disabled by default ({@code color-limits.enabled}) so it never fights another
 * chat plugin unless an operator opts in. Registration and config reads live in
 * {@link ColorStripListener}; this class only wires the listener in.</p>
 */
public final class ColorLimitsModule implements EssModule {

    @Override
    public String name() {
        return "colorlimits";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new ColorStripListener(plugin), plugin);
    }
}
