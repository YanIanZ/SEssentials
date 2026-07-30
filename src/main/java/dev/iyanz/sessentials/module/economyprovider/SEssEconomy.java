package dev.iyanz.sessentials.module.economyprovider;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import dev.iyanz.sessentials.SEssentialsPlugin;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/**
 * SEssentials' own implementation of Vault's {@link Economy} SPI, serving balances from
 * {@link BalanceStore}. Registered at {@link org.bukkit.plugin.ServicePriority#Low} so it
 * stays dormant behind CMI while CMI is installed, and becomes the active provider once
 * CMI is removed.
 *
 * <p>There is no bank support: every {@code bank*} method returns a
 * {@link ResponseType#NOT_IMPLEMENTED} response. All accounts are global — the
 * world-scoped overloads delegate to the world-agnostic ones. The deprecated
 * player-name overloads resolve the name through {@link Bukkit#getOfflinePlayer(String)}
 * (hence the class-level {@code deprecation} suppression), matching how every other Vault
 * provider bridges the legacy API.</p>
 */
@SuppressWarnings("deprecation")
public final class SEssEconomy implements Economy {

    private static final String NO_BANK = "SEssentials has no bank support";

    private final BalanceStore balances;
    private final String symbol;
    private final String currencySingular;
    private final String currencyPlural;

    /**
     * @param plugin   the owning plugin (for currency config)
     * @param balances the balance store to serve
     */
    public SEssEconomy(SEssentialsPlugin plugin, BalanceStore balances) {
        this.balances = balances;
        this.symbol = plugin.getConfig().getString("economy.symbol", "$");
        this.currencySingular = plugin.getConfig().getString("economy.currency-singular", "coin");
        this.currencyPlural = plugin.getConfig().getString("economy.currency-plural", "coins");
    }

    // ---- Metadata -----------------------------------------------------------------

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "SEssentials";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double amount) {
        String number = String.format(Locale.US, "%,.2f", amount);
        String name = amount == 1.0D ? currencySingular : currencyPlural;
        return symbol + number + " " + name;
    }

    @Override
    public String currencyNamePlural() {
        return currencyPlural;
    }

    @Override
    public String currencyNameSingular() {
        return currencySingular;
    }

    // ---- Accounts (OfflinePlayer) -------------------------------------------------

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return balances.hasAccount(player.getUniqueId());
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String world) {
        return hasAccount(player);
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return balances.get(player.getUniqueId());
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return balances.has(player.getUniqueId(), amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String world, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        UUID id = player.getUniqueId();
        if (amount < 0) {
            return new EconomyResponse(amount, balances.get(id), ResponseType.FAILURE,
                    "Cannot withdraw a negative amount");
        }
        if (!balances.withdraw(id, amount)) {
            return new EconomyResponse(amount, balances.get(id), ResponseType.FAILURE,
                    "Insufficient funds");
        }
        return new EconomyResponse(amount, balances.get(id), ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String world, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        UUID id = player.getUniqueId();
        if (amount < 0) {
            return new EconomyResponse(amount, balances.get(id), ResponseType.FAILURE,
                    "Cannot deposit a negative amount");
        }
        balances.deposit(id, amount);
        return new EconomyResponse(amount, balances.get(id), ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String world, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        balances.createAccount(player.getUniqueId());
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String world) {
        return createPlayerAccount(player);
    }

    // ---- Accounts (legacy player-name overloads) ----------------------------------

    @Override
    public boolean hasAccount(String playerName) {
        return hasAccount(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public boolean hasAccount(String playerName, String world) {
        return hasAccount(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public double getBalance(String playerName) {
        return getBalance(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public boolean has(String playerName, double amount) {
        return has(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public boolean has(String playerName, String world, double amount) {
        return has(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String world, double amount) {
        return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String world, double amount) {
        return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return createPlayerAccount(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public boolean createPlayerAccount(String playerName, String world) {
        return createPlayerAccount(Bukkit.getOfflinePlayer(playerName));
    }

    // ---- Banks (unsupported) ------------------------------------------------------

    @Override
    public EconomyResponse createBank(String name, String player) {
        return noBank();
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return noBank();
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return noBank();
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return noBank();
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return noBank();
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return noBank();
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return noBank();
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return noBank();
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return noBank();
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return noBank();
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return noBank();
    }

    @Override
    public List<String> getBanks() {
        return List.of();
    }

    private static EconomyResponse noBank() {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, NO_BANK);
    }
}
