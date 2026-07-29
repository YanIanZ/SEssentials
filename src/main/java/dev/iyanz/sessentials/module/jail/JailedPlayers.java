package dev.iyanz.sessentials.module.jail;

import java.util.UUID;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.store.YamlStore;

/**
 * Static persistence helpers for jailed-player state, kept in the shared
 * {@code jail.yml} data store (see {@link Jails}) under {@code "jailed.<uuid>"} as
 * three separate keys: {@code .jail} (the jail name), {@code .until} (expiry as an
 * epoch-millis {@code long}, {@code 0} meaning permanent) and {@code .reason}
 * (optional, may be absent).
 */
final class JailedPlayers {

    private JailedPlayers() {
    }

    private static String base(UUID uuid) {
        return "jailed." + uuid;
    }

    /** @return whether {@code uuid} currently has a jailed-state record (expired or not). */
    static boolean isJailed(SEssentialsPlugin plugin, UUID uuid) {
        return Jails.store(plugin).contains(base(uuid) + ".jail");
    }

    /** @return the name of the jail {@code uuid} is held in, or {@code null} if not jailed. */
    static String jailName(SEssentialsPlugin plugin, UUID uuid) {
        return Jails.store(plugin).getString(base(uuid) + ".jail");
    }

    /** @return the expiry epoch-millis for {@code uuid}'s sentence, or {@code 0} if permanent/unset. */
    static long until(SEssentialsPlugin plugin, UUID uuid) {
        return Jails.store(plugin).config().getLong(base(uuid) + ".until", 0L);
    }

    /** @return the jailing reason for {@code uuid}, or {@code null} if none was given. */
    static String reason(SEssentialsPlugin plugin, UUID uuid) {
        return Jails.store(plugin).getString(base(uuid) + ".reason");
    }

    /** @return whether a sentence with expiry {@code until} (epoch millis, 0 = permanent) has expired. */
    static boolean expired(long until) {
        return until != 0L && System.currentTimeMillis() >= until;
    }

    /**
     * Records {@code uuid} as jailed in {@code jailName}, persisting the store.
     *
     * @param plugin   the owning plugin
     * @param uuid     the jailed player's UUID
     * @param jailName the jail name (should already be lower-cased and exist)
     * @param until    expiry epoch-millis, or {@code 0} for a permanent sentence
     * @param reason   the jailing reason, or {@code null} for none
     */
    static void set(SEssentialsPlugin plugin, UUID uuid, String jailName, long until, String reason) {
        YamlStore store = Jails.store(plugin);
        store.set(base(uuid) + ".jail", jailName);
        store.set(base(uuid) + ".until", until);
        store.set(base(uuid) + ".reason", reason);
        store.save();
    }

    /** Clears {@code uuid}'s jailed-state record entirely and persists the store. */
    static void clear(SEssentialsPlugin plugin, UUID uuid) {
        YamlStore store = Jails.store(plugin);
        store.remove(base(uuid));
        store.save();
    }
}
