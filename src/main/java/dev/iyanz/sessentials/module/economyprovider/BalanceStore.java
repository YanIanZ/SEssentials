package dev.iyanz.sessentials.module.economyprovider;

import java.util.UUID;
import java.util.logging.Logger;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.store.YamlStore;

/**
 * The single source of truth for SEssentials' own player balances, backed by the shared
 * {@code economy.yml} data store. Each account lives at {@code balances.<uuid>} and its
 * value is the balance encoded as a string (via {@link Double#toString(double)}), so it
 * round-trips exactly through the monitor-guarded {@link YamlStore#getString}/{@link
 * YamlStore#set} accessors (which have no {@code double} form).
 *
 * <p><strong>Money-critical thread-safety.</strong> A balance mutation is a
 * read-modify-write: reading the current balance, computing the new one, and writing it
 * back. On Folia several region threads may hit the same account at once, so every
 * compound operation here holds a single monitor — the {@code YamlStore} guards each
 * individual read/write, but only this monitor makes the whole read-modify-write atomic,
 * preventing lost updates (e.g. two concurrent deposits both reading the same "before"
 * value). Each mutation persists via {@link YamlStore#save()} (serialise-under-lock,
 * write async) so a crash can't lose a committed change.</p>
 */
public final class BalanceStore {

    /** Name of the shared YAML store backing balances ({@code economy.yml}). */
    public static final String STORE = "economy";
    /** Path prefix under which each account's balance is stored. */
    private static final String PATH_PREFIX = "balances.";

    private final YamlStore store;
    private final Logger log;
    /** Balance handed out for an account that doesn't exist yet. */
    private final double startingBalance;
    /** Serialises read-modify-write so concurrent region threads can't lose updates. */
    private final Object lock = new Object();

    /**
     * @param plugin the owning plugin (for the {@code economy} store and config defaults)
     */
    public BalanceStore(SEssentialsPlugin plugin) {
        this.store = plugin.stores().get(STORE);
        this.log = plugin.getLogger();
        this.startingBalance = plugin.getConfig().getDouble("economy.starting-balance", 0.0);
    }

    /**
     * @param uuid a player id
     * @return the persistence path holding that player's balance — the storage contract
     *         (store {@value #STORE}, path {@code balances.<uuid>})
     */
    public static String path(UUID uuid) {
        return PATH_PREFIX + uuid;
    }

    /** @return whether {@code uuid} has an account (never throws). */
    public boolean hasAccount(UUID uuid) {
        return store.contains(path(uuid));
    }

    /** @return the player's balance, or the configured starting balance if no account exists. */
    public double get(UUID uuid) {
        synchronized (lock) {
            return read(uuid);
        }
    }

    /** Sets the player's balance to {@code amount}, persisting the change. */
    public void set(UUID uuid, double amount) {
        synchronized (lock) {
            write(uuid, amount);
        }
    }

    /** @return whether the player has at least {@code amount}. */
    public boolean has(UUID uuid, double amount) {
        synchronized (lock) {
            return read(uuid) >= amount;
        }
    }

    /**
     * Adds {@code amount} to the player's balance.
     *
     * @return {@code true} on success; {@code false} if {@code amount} is negative
     */
    public boolean deposit(UUID uuid, double amount) {
        if (amount < 0) {
            return false;
        }
        synchronized (lock) {
            write(uuid, read(uuid) + amount);
            return true;
        }
    }

    /**
     * Removes {@code amount} from the player's balance.
     *
     * @return {@code true} on success; {@code false} if {@code amount} is negative or the
     *         balance is insufficient
     */
    public boolean withdraw(UUID uuid, double amount) {
        if (amount < 0) {
            return false;
        }
        synchronized (lock) {
            double balance = read(uuid);
            if (balance < amount) {
                return false;
            }
            write(uuid, balance - amount);
            return true;
        }
    }

    /**
     * Creates the player's account, seeding it with the configured starting balance if it
     * doesn't already exist.
     *
     * @return {@code true} if a new account was created, {@code false} if one already existed
     */
    public boolean createAccount(UUID uuid) {
        synchronized (lock) {
            if (store.contains(path(uuid))) {
                return false;
            }
            write(uuid, startingBalance);
            return true;
        }
    }

    /** Reads the stored balance. Caller must hold {@link #lock}. */
    private double read(UUID uuid) {
        String path = path(uuid);
        if (!store.contains(path)) {
            return startingBalance;
        }
        String raw = store.getString(path);
        if (raw == null) {
            return startingBalance;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            // A stored account that can't be parsed is corrupt. Never invent funds: report
            // it loudly and treat as zero rather than silently handing out a balance.
            log.severe("Corrupt balance for " + uuid + " (" + raw + ") — treating as 0.");
            return 0.0;
        }
    }

    /** Writes and persists the balance. Caller must hold {@link #lock}. */
    private void write(UUID uuid, double amount) {
        store.set(path(uuid), Double.toString(amount));
        store.save();
    }
}
