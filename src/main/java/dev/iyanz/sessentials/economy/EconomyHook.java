package dev.iyanz.sessentials.economy;

import java.util.UUID;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Thin wrapper over a Vault {@link Economy} provider (e.g. CMI, EssentialsX). The
 * provider is resolved lazily so SEssentials works even when the economy plugin
 * registers <em>after</em> us. If no provider is present, {@link #available()} is
 * {@code false} and monetary operations are safe no-ops.
 *
 * <p><strong>Crash-safety.</strong> Under Paper's {@code paper-plugin.yml} classloader
 * isolation, referencing {@code net.milkbowl.vault.economy.Economy} when Vault is absent
 * (or not on our classpath) raises a {@link NoClassDefFoundError} — an {@link Error}, not
 * an {@link Exception}, so it would slip past the per-module {@code catch (Exception)} in
 * the plugin bootstrap and abort the whole enable ("can't read Vault"). Every access to
 * the Vault type here is therefore guarded against {@link Throwable}: a missing/incompatible
 * Vault degrades to "no economy" instead of taking the plugin down. Vault is declared with
 * {@code join-classpath: true} in {@code paper-plugin.yml} so its classes are visible when
 * it <em>is</em> installed.</p>
 */
public final class EconomyHook {

    private Economy economy;
    /** True once we've confirmed the Vault API type can't be loaded (Vault absent). */
    private boolean vaultMissing;

    /** Resolves and caches the provider on first availability; never throws. */
    private Economy economy() {
        if (economy != null || vaultMissing) {
            return economy;
        }
        try {
            RegisteredServiceProvider<Economy> rsp =
                    Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
            }
        } catch (NoClassDefFoundError vaultNotOnClasspath) {
            // Vault isn't installed / not on our classpath — stop trying to touch its type.
            vaultMissing = true;
        } catch (Throwable ignored) {
            // Any other lookup hiccup: treat as "no economy" for this call, retry later.
        }
        return economy;
    }

    /**
     * Logs the resolved Vault economy status once, at enable, without ever throwing.
     * Call from the plugin bootstrap so admins can see whether the hook took.
     *
     * @param plugin the owning plugin (for its logger)
     */
    public void logStatus(Plugin plugin) {
        Logger log = plugin.getLogger();
        try {
            Economy e = economy();
            if (e != null) {
                log.info("Hooked into Vault economy provider: " + e.getName());
            } else if (vaultMissing) {
                log.info("Vault not installed — economy features are disabled.");
            } else {
                log.info("Vault present but no economy provider registered yet — "
                        + "will hook automatically once one registers.");
            }
        } catch (Throwable ignored) {
            log.info("Vault economy unavailable — economy features are disabled.");
        }
    }

    /** @return whether an economy provider is available. Never throws. */
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
