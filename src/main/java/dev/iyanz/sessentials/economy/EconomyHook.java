package dev.iyanz.sessentials.economy;

import java.util.UUID;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Thin wrapper over a Vault {@link Economy} provider (e.g. CMI, EssentialsX). The
 * provider is resolved lazily so SEssentials works even when the economy plugin
 * registers <em>after</em> us. If no provider is present, {@link #available()} is
 * {@code false} and monetary operations are safe no-ops.
 */
public final class EconomyHook {

    private Economy economy;

    /** Resolves and caches the provider on first availability. */
    private Economy economy() {
        if (economy == null) {
            RegisteredServiceProvider<Economy> rsp =
                    Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
            }
        }
        return economy;
    }

    /** @return whether an economy provider is available. */
    public boolean available() {
        return economy() != null;
    }

    private OfflinePlayer player(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid);
    }

    /** @return the player's balance (0 if unavailable). */
    public double balance(UUID player) {
        Economy e = economy();
        return e != null ? e.getBalance(player(player)) : 0.0;
    }

    /** @return whether the player has at least {@code amount}. */
    public boolean has(UUID player, double amount) {
        Economy e = economy();
        return e != null && e.has(player(player), amount);
    }

    /** Removes {@code amount} from the player's balance. @return success */
    public boolean withdraw(UUID player, double amount) {
        Economy e = economy();
        return e != null && e.withdrawPlayer(player(player), amount).transactionSuccess();
    }

    /** Adds {@code amount} to the player's balance. @return success */
    public boolean deposit(UUID player, double amount) {
        Economy e = economy();
        return e != null && e.depositPlayer(player(player), amount).transactionSuccess();
    }

    /** @return a currency-formatted string for {@code amount}. */
    public String format(double amount) {
        Economy e = economy();
        return e != null ? e.format(amount) : String.format("%,.2f", amount);
    }
}
