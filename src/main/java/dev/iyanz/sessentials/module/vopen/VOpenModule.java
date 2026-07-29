package dev.iyanz.sessentials.module.vopen;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Virtual-UI openers for interactive workstation screens that are <em>not</em> already
 * covered by the {@code playerstate} module's workbench commands: the enchanting table
 * ({@code /enchanttable}), brewing stand ({@code /brewingstand}), furnace family
 * ({@code /furnace}, {@code /blastfurnace}, {@code /smoker}) and hopper
 * ({@code /hopper}).
 *
 * <p>Each command opens a fresh, block-less menu directly on the client via Paper's
 * {@link org.bukkit.inventory.MenuType} API. The screens are ephemeral: because they
 * are not backed by a real block entity, anything placed in them is discarded when the
 * screen closes (they behave as scratch/dispose surfaces rather than persistent
 * containers). The enchanting table and hopper remain fully interactive; the furnace
 * and brewing screens open as UIs but do not tick (no smelting/brewing occurs without a
 * backing block), which is acceptable for a virtual opener.</p>
 *
 * <p>All commands are player-only and gated behind {@code sessentials.<command>}. Each
 * runs entirely on the sender's own region thread (the command executor already is), so
 * no scheduler hop is required.</p>
 */
public final class VOpenModule implements EssModule {

    @Override
    public String name() {
        return "vopen";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(VOpenCommands::register);
    }
}
