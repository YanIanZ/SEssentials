package dev.iyanz.sessentials.module.items;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Item and inventory administration commands: giving items, topping up held stacks,
 * enchanting/unenchanting, renaming and re-loring items, and bulk inventory
 * maintenance (clear/condense).
 *
 * <p>Every command operates on the sender's held item or inventory unless a
 * {@code [player]} target is supplied, in which case the mutation is hopped onto
 * that player's Folia region thread. See the individual command classes in this
 * package for details:</p>
 * <ul>
 *   <li>{@link GiveMoreCommands} — {@code give}/{@code i}/{@code item}, {@code more}</li>
 *   <li>{@link EnchantCommands} — {@code enchant}, {@code unenchant}/{@code removeenchant}</li>
 *   <li>{@link ItemTextCommands} — {@code rename}/{@code itemname}, {@code lore}</li>
 *   <li>{@link InventoryCommands} — {@code clear}/{@code ci}/{@code clearinventory}, {@code condense}</li>
 * </ul>
 */
public final class ItemsModule implements EssModule {

    @Override
    public String name() {
        return "items";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        GiveMoreCommands.register(plugin);
        EnchantCommands.register(plugin);
        ItemTextCommands.register(plugin);
        InventoryCommands.register(plugin);
    }
}
