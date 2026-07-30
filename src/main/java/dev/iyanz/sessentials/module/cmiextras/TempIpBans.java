package dev.iyanz.sessentials.module.cmiextras;

import java.util.concurrent.ConcurrentHashMap;

import dev.iyanz.sessentials.store.YamlStore;

/**
 * Registry of temporary IP bans: a thread-safe in-memory map of {@code address →
 * expiry (epoch millis)} backed by the {@code tempipban} {@link YamlStore}.
 *
 * <p>The {@link ConcurrentHashMap} snapshot exists so the asynchronous pre-login
 * listener can check bans off-thread without ever touching the YAML store; the store
 * is only written from command (region) threads. Expired entries are dropped from the
 * snapshot lazily on lookup and compacted out of the store at construction (i.e. on
 * every server start), so stale YAML entries never outlive a restart.</p>
 *
 * <p>Each ban is persisted under {@code bans.<sanitized>} where the sanitized key
 * replaces {@code '.'} and {@code ':'} (illegal in YAML path segments) with
 * {@code '_'}; the verbatim address is kept under the {@code .ip} child.</p>
 */
final class TempIpBans {

    private final YamlStore store;
    private final ConcurrentHashMap<String, Long> expiries = new ConcurrentHashMap<>();

    /**
     * Loads all unexpired bans from the store and compacts away expired ones.
     *
     * @param store the {@code tempipban} data store
     */
    TempIpBans(YamlStore store) {
        this.store = store;
        long now = System.currentTimeMillis();
        boolean purged = false;
        for (String key : store.keys("bans")) {
            String ip = store.getString("bans." + key + ".ip");
            long expiry = store.getLong("bans." + key + ".expiry", 0L);
            if (ip == null || ip.isBlank() || expiry <= now) {
                store.remove("bans." + key);
                purged = true;
            } else {
                expiries.put(ip, expiry);
            }
        }
        if (purged) {
            store.save();
        }
    }

    /**
     * Looks up the active ban expiry for an address. Safe to call from any thread —
     * it only reads (and lazily cleans) the in-memory snapshot, never the store.
     *
     * @param ip a raw host address (may be {@code null})
     * @return the epoch-millis expiry of an unexpired ban, or {@code null} if the
     *         address is not banned (or its ban has lapsed)
     */
    Long activeExpiry(String ip) {
        if (ip == null) {
            return null;
        }
        Long expiry = expiries.get(ip);
        if (expiry == null) {
            return null;
        }
        if (expiry <= System.currentTimeMillis()) {
            expiries.remove(ip, expiry);
            return null;
        }
        return expiry;
    }

    /**
     * Records (or extends) a temporary ban for an address, updating both the snapshot
     * and the persistent store. Call from a command (region) thread only.
     *
     * @param ip           the raw host address to ban
     * @param expiryMillis when the ban lapses, epoch millis
     * @param playerName   the name of the banned player (for auditing)
     * @param by           the name of the sender issuing the ban (for auditing)
     */
    void add(String ip, long expiryMillis, String playerName, String by) {
        expiries.put(ip, expiryMillis);
        String key = "bans." + sanitize(ip);
        store.set(key + ".ip", ip);
        store.set(key + ".expiry", expiryMillis);
        store.set(key + ".player", playerName);
        store.set(key + ".by", by);
        store.save();
    }

    /**
     * Sanitizes an address into a legal YAML path segment.
     *
     * @param ip the raw host address
     * @return the address with {@code '.'} and {@code ':'} replaced by {@code '_'}
     */
    static String sanitize(String ip) {
        return ip.replace('.', '_').replace(':', '_');
    }
}
