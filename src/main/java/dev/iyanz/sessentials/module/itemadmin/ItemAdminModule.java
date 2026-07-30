package dev.iyanz.sessentials.module.itemadmin;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Advanced item administration commands operating on the sender's held item:
 * <ul>
 *   <li>{@link MetaToggleCommands} — {@code hideflags} (toggle all item flags),
 *       {@code unbreakable} (toggle unbreakability), {@code itemcmdata}
 *       (set or clear custom model data)</li>
 *   <li>{@link TrimCommand} — {@code trim} (apply an armor trim pattern/material)</li>
 *   <li>{@link MobHeadCommand} — {@code mobhead} (give a vanilla mob head)</li>
 * </ul>
 *
 * <p>All commands are player-only and act on the main-hand item; the command
 * executor already runs on the sender's Folia region thread, so the sender's
 * own inventory may be mutated directly.</p>
 */
public final class ItemAdminModule implements EssModule {

    @Override
    public String name() {
        return "itemadmin";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        MetaToggleCommands.register(plugin);
        TrimCommand.register(plugin);
        MobHeadCommand.register(plugin);
    }
}
