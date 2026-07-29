package dev.iyanz.sessentials.module.nbt;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Read-only NBT / item-and-entity inspection tools: {@code /itemnbt} (alias
 * {@code /iteminfo}) and {@code /entitynbt} (alias {@code /entityinfo2}), both gated
 * behind {@code sessentials.nbt}.
 *
 * <p>Every command in this module only reads state already resolved on the sender's
 * own client input (held item, crosshair ray-trace) and reports it back — nothing
 * here mutates game state or persists data, so no Folia scheduler hop is needed
 * beyond running on the sender's own region thread.</p>
 */
public final class NbtModule implements EssModule {

    @Override
    public String name() {
        return "nbt";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        ItemNbtCommand.register(plugin);
        EntityNbtCommand.register(plugin);
    }
}
