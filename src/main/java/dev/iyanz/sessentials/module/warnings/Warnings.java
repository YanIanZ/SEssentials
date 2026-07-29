package dev.iyanz.sessentials.module.warnings;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.iyanz.sessentials.store.YamlStore;

/**
 * Persistence and formatting helper for the {@code warnings} data store.
 *
 * <p>Each warning is encoded as a single pipe-delimited string,
 * {@code "<staffName>|<epochMillis>|<reason>"}, inside a growing YAML list under
 * {@code "<uuid>.list"}. The reason portion is split with a limit so it may itself
 * contain {@code |} characters without corrupting the encoding.</p>
 */
final class Warnings {

    private static final String LIST_SUFFIX = ".list";

    private Warnings() {
    }

    /** A single warning: which staff member issued it, when (epoch millis), and why. */
    record Entry(String staffName, long epochMillis, String reason) {
    }

    /** @return the raw, encoded warning entries for {@code uuid}, oldest first (never {@code null}). */
    private static List<String> raw(YamlStore store, UUID uuid) {
        return store.config().getStringList(uuid + LIST_SUFFIX);
    }

    /** @return {@code uuid}'s warnings, decoded oldest-first, skipping any malformed lines. */
    static List<Entry> list(YamlStore store, UUID uuid) {
        List<String> lines = raw(store, uuid);
        List<Entry> entries = new ArrayList<>(lines.size());
        for (String line : lines) {
            Entry entry = parse(line);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    /** Appends a new warning (stamped "now") for {@code uuid} and saves the store. */
    static void add(YamlStore store, UUID uuid, String staffName, String reason) {
        String path = uuid + LIST_SUFFIX;
        List<String> lines = new ArrayList<>(store.config().getStringList(path));
        lines.add(encode(staffName, System.currentTimeMillis(), reason));
        store.set(path, lines);
        store.save();
    }

    /**
     * Removes the warning at {@code index} (0-based, oldest-first order) for {@code uuid}
     * and saves the store.
     *
     * @return {@code true} if an entry existed at {@code index} and was removed
     */
    static boolean removeAt(YamlStore store, UUID uuid, int index) {
        String path = uuid + LIST_SUFFIX;
        List<String> lines = new ArrayList<>(store.config().getStringList(path));
        if (index < 0 || index >= lines.size()) {
            return false;
        }
        lines.remove(index);
        store.set(path, lines);
        store.save();
        return true;
    }

    /** Deletes every warning for {@code uuid} and saves the store. */
    static void clear(YamlStore store, UUID uuid) {
        store.remove(uuid + LIST_SUFFIX);
        store.save();
    }

    /** @return a short {@code "2d 3h"}-style rendering of how long ago {@code epochMillis} was. */
    static String relative(long epochMillis) {
        long totalSeconds = Math.max(0L, (System.currentTimeMillis() - epochMillis) / 1000L);
        long days = totalSeconds / 86_400L;
        long hours = (totalSeconds % 86_400L) / 3_600L;
        long minutes = (totalSeconds % 3_600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

    private static String encode(String staffName, long epochMillis, String reason) {
        return staffName + "|" + epochMillis + "|" + reason;
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
