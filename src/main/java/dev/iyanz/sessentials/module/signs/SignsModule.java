package dev.iyanz.sessentials.module.signs;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Two listener-driven sign features; no commands are registered.
 *
 * <ul>
 *   <li><b>Coloured signs</b> — an editor holding {@code sessentials.sign.color} may
 *       write {@code &}-codes or MiniMessage tags on any of the 4 lines; each line is
 *       re-parsed and set back as a rich {@link net.kyori.adventure.text.Component}.
 *       See {@link SignChangeListener}.</li>
 *   <li><b>Action signs</b> — a first line matching a recognised {@link SignTag}
 *       (e.g. {@code [warp]}) turns the sign into a right-click shortcut (warp, spawn,
 *       command, heal, feed or kit). Creating one requires
 *       {@code sessentials.signs.create}; using one requires the tag's own
 *       {@code sessentials.signs.use.<tag>} permission. See {@link SignChangeListener}
 *       (creation gate) and {@link SignInteractListener} (execution).</li>
 * </ul>
 */
public final class SignsModule implements EssModule {

    @Override
    public String name() {
        return "signs";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new SignChangeListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new SignInteractListener(plugin), plugin);
    }
}
