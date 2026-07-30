package dev.iyanz.sessentials.module.itemcmds;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Item-centric convenience commands that complement the core items module:
 * <ul>
 *   <li>{@link EnchantBookCommand} — {@code /enchantbook &lt;enchant&gt; [level]}: hands the
 *       sender an enchanted book carrying the chosen stored enchantment.</li>
 *   <li>{@link GiveHandCommand} — {@code /givehand &lt;player&gt;}: gives a copy of the
 *       sender's held item to another player.</li>
 *   <li>{@link ClearInvCommand} — {@code /clearinv &lt;player&gt;}: wipes another player's
 *       inventory (self-clearing is already provided by {@code /clear} / {@code /ci}).</li>
 * </ul>
 *
 * <p>All target-affecting mutations hop onto the target's Folia region thread via
 * {@code Schedulers.entity}; sender-only work runs directly in the command executor,
 * which already executes on the sender's region thread.</p>
 */
public final class ItemCmdsModule implements EssModule {

    @Override
    public String name() {
        return "itemcmds";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        EnchantBookCommand.register(plugin);
        GiveHandCommand.register(plugin);
        ClearInvCommand.register(plugin);
    }
}
