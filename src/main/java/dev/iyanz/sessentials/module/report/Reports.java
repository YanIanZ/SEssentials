package dev.iyanz.sessentials.module.report;

import java.util.ArrayList;
import java.util.List;

import dev.iyanz.sessentials.store.YamlStore;

/**
 * Persistence and formatting helper for the {@code reports} data store.
 *
 * <p>Unlike per-player systems such as warnings, reports live in a single, server-wide
 * queue: every report is appended to one growing YAML string list under {@code "list"}.
 * Each entry is encoded as a pipe-delimited string,
 * {@code "<reporterName>|<reportedName>|<epochMillis>|<reason>"}. The reason is the final
 * field and is split with a limit, so it may itself contain {@code |} characters without
 * corrupting the encoding.</p>
 */
final class Reports {

    /** YAML path holding the growing list of encoded report entries. */
    private static final String LIST_PATH = "list";

    private Reports() {
    }

    /**
     * A single report: who filed it, who it targets, when (epoch millis) and why.
     *
     * @param reporterName the name of the sender who filed the report
     * @param reportedName the (canonical, where known) name of the reported player
     * @param epochMillis  when the report was filed
     * @param reason       the free-text reason (untrusted; render defensively)
     */
    record Entry(String reporterName, String reportedName, long epochMillis, String reason) {
    }

    /** @return the raw, encoded report lines, oldest first (never {@code null}). */
    private static List<String> raw(YamlStore store) {
        return store.config().getStringList(LIST_PATH);
    }

    /** @return the number of stored report lines. */
    static int count(YamlStore store) {
        return raw(store).size();
    }

    /**
     * Appends a new report (stamped "now") to the queue and saves the store.
     *
     * @return the {@link Entry} that was persisted, for immediate notification
     */
    static Entry add(YamlStore store, String reporterName, String reportedName, String reason) {
        Entry entry = new Entry(reporterName, reportedName, System.currentTimeMillis(), reason);
        List<String> lines = new ArrayList<>(raw(store));
        lines.add(encode(entry));
        store.set(LIST_PATH, lines);
        store.save();
        return entry;
    }

    /**
     * @param limit the maximum number of entries to return
     * @return up to {@code limit} reports, newest first, skipping any malformed lines
     */
    static List<Entry> recent(YamlStore store, int limit) {
        List<String> lines = raw(store);
        List<Entry> entries = new ArrayList<>(Math.min(limit, lines.size()));
        for (int i = lines.size() - 1; i >= 0 && entries.size() < limit; i--) {
            Entry entry = parse(lines.get(i));
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    /** Deletes every report and saves the store. */
    static void clear(YamlStore store) {
        store.remove(LIST_PATH);
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

    private static String encode(Entry entry) {
        return entry.reporterName() + "|" + entry.reportedName() + "|" + entry.epochMillis() + "|" + entry.reason();
    }

    private static Entry parse(String line) {
        if (line == null) {
            return null;
        }
        String[] parts = line.split("\\|", 4);
        if (parts.length < 4) {
            return null;
        }
        try {
            return new Entry(parts[0], parts[1], Long.parseLong(parts[2]), parts[3]);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
