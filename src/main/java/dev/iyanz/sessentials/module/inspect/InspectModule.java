package dev.iyanz.sessentials.module.inspect;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Staff inspection and lookup tools: {@code /whois}, {@code /blockinfo},
 * {@code /entityinfo}, {@code /checkperm}, {@code /checkexp}, {@code /banlist} and
 * {@code /ip} (alias {@code /getip}).
 *
 * <p>Every command in this module is a read-only report — nothing here mutates game
 * state, persists data, or requires Folia scheduler hops beyond the ones documented
 * on each command (self-facing block/entity ray-traces run on the sender's own
 * region thread; other-player lookups only read simple, already-resolved fields).</p>
 */
public final class InspectModule implements EssModule {

    @Override
    public String name() {
        return "inspect";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        WhoisCommand.register(plugin);
        BlockInfoCommand.register(plugin);
        EntityInfoCommand.register(plugin);
        CheckPermCommand.register(plugin);
        CheckExpCommand.register(plugin);
        BanListCommand.register(plugin);
        IpCommand.register(plugin);
    }
}
