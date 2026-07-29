package dev.iyanz.sessentials.module.ipban;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import dev.iyanz.sessentials.store.YamlStore;

/**
 * The banned-IP registry: a thread-safe in-memory snapshot of every banned address
 * backed by the {@code ipban} {@link YamlStore}.
 *
 * <p>The in-memory {@link Set} is a {@link ConcurrentHashMap}-backed key set so the
 * asynchronous pre-login listener can read it off any thread without touching the
 * YAML store. The store is the durable source of truth and is only ever read/written
 * from command (region/main) threads; the memory set is loaded once on construction
 * and kept in lock-step with the store on every {@link #add} / {@link #remove}.</p>
 *
 * <p>Each address is persisted under {@code banned.<sanitized>} where the sanitized
 * key replaces {@code '.'} and {@code ':'} (both illegal in YAML path segments) with
 * {@code '_'}. The original address is stored verbatim under the {@code .ip} child so
 * it can always be recovered exactly.</p>
 */
final class BannedIps {

    private final YamlStore store;
    private final Set<String> banned = ConcurrentHashMap.newKeySet();

    /**
     * @param store the {@code ipban} data store; its existing entries are loaded now
     */
    BannedIps(YamlStore store) {
        this.store = store;
        for (String key : store.keys("banned")) {
            String ip = store.getString("banned." + key + ".ip");
            if (ip != null && !ip.isBlank()) {
                banned.add(ip);
            }
        }
    }

    /**
     * @param ip a raw host address (may be {@code null})
     * @return {@code true} if the address is currently banned
     */
    boolean isBanned(String ip) {
        return ip != null && banned.contains(ip);
    }

    /**
     * Bans an address, updating both the in-memory snapshot and the persistent store.
     *
     * @param ip the raw host address to ban
     * @param by the name of the sender issuing the ban (for auditing)
     * @return {@code true} if the address was newly banned, {@code false} if it was
     *         already present
     */
    boolean add(String ip, String by) {
        if (!banned.add(ip)) {
            return false;
        }
        String sani = sanitize(ip);
        store.set("banned." + sani + ".ip", ip);
        store.set("banned." + sani + ".time", System.currentTimeMillis());
        store.set("banned." + sani + ".by", by);
        store.save();
        return true;
    }

    /**
     * Lifts the ban on an address from both the snapshot and the store.
     *
     * @param ip the raw host address to unban
     * @return {@code true} if the address had been banned, {@code false} otherwise
     */
    boolean remove(String ip) {
        boolean inMemory = banned.remove(ip);
        String path = "banned." + sanitize(ip);
        boolean inStore = store.contains(path);
        if (inStore) {
            store.remove(path);
        }
        if (inMemory || inStore) {
            store.save();
            return true;
        }
        return false;
    }

    /**
     * Lists every banned address, resolved back to its verbatim form via the store.
     *
     * @return a sorted, mutable list of banned host addresses
     */
    List<String> list() {
        List<String> out = new ArrayList<>();
        for (String key : store.keys("banned")) {
            String ip = store.getString("banned." + key + ".ip");
            out.add(ip != null ? ip : key);
        }
        Collections.sort(out);
        return out;
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
