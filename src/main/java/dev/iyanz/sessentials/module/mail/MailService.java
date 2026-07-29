package dev.iyanz.sessentials.module.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.store.YamlStore;

/**
 * Persistence helper for the {@code "mail"} data store: encodes/decodes mail
 * entries, appends new mail, clears a player's mailbox, and stamps/consults the
 * "last read" timestamp used to compute unread counts.
 *
 * <p>Each mail entry is stored as a single pipe-delimited string,
 * {@code "<fromName>|<epochMillis>|<text>"}, inside a YAML list under
 * {@code "<uuid>.messages"}. The text portion is split with a limit so a message
 * may itself contain {@code |} characters without corrupting the encoding.</p>
 */
final class MailService {

    private static final String MESSAGES_SUFFIX = ".messages";
    private static final String LAST_READ_SUFFIX = ".lastRead";

    private MailService() {
    }

    /** A single piece of mail: who sent it, when (epoch millis), and its text. */
    record Entry(String fromName, long epochMillis, String text) {
    }

    /** @return the shared store backing all mail data. */
    static YamlStore store(SEssentialsPlugin plugin) {
        return plugin.stores().get("mail");
    }

    /** @return the raw, encoded mail entries for {@code uuid}, oldest first (never {@code null}). */
    static List<String> rawMessages(YamlStore store, UUID uuid) {
        return store.getStringList(uuid + MESSAGES_SUFFIX);
    }

    /** @return {@code raw} decoded into {@link Entry} objects, skipping any malformed lines. */
    static List<Entry> decode(List<String> raw) {
        List<Entry> entries = new ArrayList<>(raw.size());
        for (String line : raw) {
            Entry entry = parse(line);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    /** Appends a new mail entry (stamped "now") for {@code uuid} and saves the store. */
    static void add(SEssentialsPlugin plugin, UUID uuid, String fromName, String text) {
        YamlStore store = store(plugin);
        String path = uuid + MESSAGES_SUFFIX;
        // Read-modify-write through the monitor-guarded typed accessors so a concurrent
        // /mail send (or the async save) can't race the raw FileConfiguration and lose
        // messages or throw a ConcurrentModificationException.
        List<String> messages = new ArrayList<>(store.getStringList(path));
        messages.add(encode(fromName, System.currentTimeMillis(), text));
        store.set(path, messages);
        store.save();
    }

    /** Deletes all mail and read-tracking state for {@code uuid} and saves the store. */
    static void clear(SEssentialsPlugin plugin, UUID uuid) {
        YamlStore store = store(plugin);
        store.remove(uuid + MESSAGES_SUFFIX);
        store.remove(uuid + LAST_READ_SUFFIX);
        store.save();
    }

    /** Stamps "now" as the last time {@code uuid} read their mail, and saves the store. */
    static void markRead(SEssentialsPlugin plugin, UUID uuid) {
        YamlStore store = store(plugin);
        store.set(uuid + LAST_READ_SUFFIX, Long.toString(System.currentTimeMillis()));
        store.save();
    }

    /** @return how many of {@code uuid}'s mail entries arrived after their last {@code /mail read}. */
    static int unreadCount(YamlStore store, UUID uuid) {
        long lastRead = lastRead(store, uuid);
        int count = 0;
        for (Entry entry : decode(rawMessages(store, uuid))) {
            if (entry.epochMillis() > lastRead) {
                count++;
            }
        }
        return count;
    }

    private static long lastRead(YamlStore store, UUID uuid) {
        String raw = store.getString(uuid + LAST_READ_SUFFIX);
        if (raw == null) {
            return 0L;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private static String encode(String fromName, long epochMillis, String text) {
        return fromName + "|" + epochMillis + "|" + text;
    }

    private static Entry parse(String line) {
        if (line == null) {
            return null;
        }
        String[] parts = line.split("\\|", 3);
        if (parts.length < 3) {
            return null;
        }
        try {
            return new Entry(parts[0], Long.parseLong(parts[1]), parts[2]);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
