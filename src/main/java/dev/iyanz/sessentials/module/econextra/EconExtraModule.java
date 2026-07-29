package dev.iyanz.sessentials.module.econextra;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Extra economy features layered on the core {@code economy} module: item-worth
 * pricing ({@code /worth}), selling the held item for its configured worth
 * ({@code /sellhand}, alias {@code /sell hand}), and money cheques ({@code /cheque})
 * that can be handed off and redeemed by right-clicking.
 *
 * <p>Every command requires a Vault economy provider to be present; without one, they
 * report an error instead of acting. See {@link WorthCommands}, {@link ChequeCommands}
 * and {@link ChequeListener} for the individual features.</p>
 */
public final class EconExtraModule implements EssModule {

    @Override
    public String name() {
        return "econextra";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new ChequeListener(plugin), plugin);
        WorthCommands.register(plugin);
        ChequeCommands.register(plugin);
    }
}
