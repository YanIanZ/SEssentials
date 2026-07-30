package dev.iyanz.sessentials.module.economyprovider;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;

/**
 * Registers SEssentials as a Vault economy provider so it can eventually replace CMI as
 * the server's economy backend.
 *
 * <p><strong>Why {@link ServicePriority#Low}.</strong> The provider is registered below
 * the {@code Normal} priority CMI uses, so while CMI is still installed Vault keeps CMI as
 * the <em>active</em> provider and nothing changes for live balances. Once CMI is removed
 * SEssentials becomes the sole provider and serves the balances migrated into its own
 * {@link BalanceStore} (see the {@code /sess import} flow). This lets an admin migrate and
 * cut over with zero downtime.</p>
 *
 * <p><strong>Crash-safety.</strong> Referencing the Vault {@link Economy} type when Vault
 * is absent raises a {@link NoClassDefFoundError} (an {@link Error}, not an
 * {@link Exception}), which would slip past the per-module {@code catch (Exception)} in the
 * bootstrap and abort the whole enable. Registration is therefore guarded by a Vault
 * presence check <em>and</em> wrapped in {@code catch (Throwable)} — a missing or
 * incompatible Vault simply means "no provider registered", never a failed startup. This
 * mirrors {@code EconomyHook}.</p>
 */
public final class EconomyProviderModule implements EssModule {

    /** The registered provider instance, kept so it can be unregistered on disable. */
    private SEssEconomy provider;

    @Override
    public String name() {
        return "economyprovider";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().info("Vault not installed — SEssentials economy provider not registered.");
            return;
        }
        try {
            BalanceStore balances = new BalanceStore(plugin);
            this.provider = new SEssEconomy(plugin, balances);
            plugin.getServer().getServicesManager().register(
                    Economy.class, provider, plugin, ServicePriority.Low);
            plugin.getLogger().info("Registered SEssentials as a Vault economy provider "
                    + "(priority Low — an existing provider such as CMI stays active until removed).");
        } catch (Throwable t) {
            this.provider = null;
            plugin.getLogger().warning("Could not register SEssentials economy provider: " + t);
        }
    }

    @Override
    public void disable(SEssentialsPlugin plugin) {
        if (provider == null) {
            return;
        }
        try {
            plugin.getServer().getServicesManager().unregister(Economy.class, provider);
        } catch (Throwable ignored) {
            // best-effort unregister on shutdown/reload
        }
        provider = null;
    }
}
