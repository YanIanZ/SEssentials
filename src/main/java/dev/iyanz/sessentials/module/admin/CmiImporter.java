package dev.iyanz.sessentials.module.admin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.module.economyprovider.BalanceStore;
import dev.iyanz.sessentials.store.YamlStore;

/**
 * Imports player data from CMI's {@code cmi.sqlite.db}:
 * <ul>
 *   <li><b>Homes</b> — {@code users.Homes} JSON ({@code {"HomeName":["world:x:y:z:yaw:pitch","icon"]}})
 *       → {@code <uuid>.homes.<name>} in the {@code home} store;</li>
 *   <li><b>Nicknames</b> — {@code users.Nickname} → {@code <uuid>.nick} in the {@code identity}
 *       store (legacy colour codes are fine — the nick module converts them on render);</li>
 *   <li><b>Balances</b> — {@code users.Balance} → written directly into SEssentials' own
 *       {@code economy} store at {@code balances.<uuid>} (so a migration populates our
 *       store even while CMI is still the active Vault provider). An existing SEssentials
 *       balance is never overwritten, and a marker under {@code balance.<uuid>} in the
 *       {@code import} store records the once-only migration;</li>
 *   <li><b>Warps</b> — the {@code warps} table (name + {@code world:x:y:z:yaw:pitch} location)
 *       → {@code warps.<name>} in the {@code warp} store.</li>
 * </ul>
 * Columns/tables that don't exist in the database are skipped cleanly, and existing SEssentials
 * entries are never overwritten. Runs entirely off the main thread.
 */
final class CmiImporter {

    private final SEssentialsPlugin plugin;

    CmiImporter(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    void run(File dbFile, Consumer<String> report) throws Exception {
        if (!dbFile.isFile()) {
            report.accept("CMI database not found at " + dbFile.getPath() + ".");
            return;
        }
        // Ensure the bundled driver is registered (some classloaders need this).
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {
            // driver auto-registers via META-INF/services on most setups
        }

        YamlStore homeStore = plugin.stores().get("home");
        YamlStore identity = plugin.stores().get("identity");
        YamlStore warpStore = plugin.stores().get("warp");
        YamlStore importLog = plugin.stores().get("import");
        YamlStore economyStore = plugin.stores().get(BalanceStore.STORE);

        int users = 0;
        int homes = 0;
        int homesSkipped = 0;
        int nicknames = 0;
        int balances = 0;
        int warps = 0;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath())) {
            Set<String> cols = columns(conn, "users");
            if (!cols.contains("player_uuid")) {
                report.accept("CMI users table has no player_uuid column — nothing imported.");
                return;
            }
            boolean hasHomes = cols.contains("homes");
            boolean hasNick = cols.contains("nickname");
            boolean hasBalance = cols.contains("balance");

            List<String> select = new ArrayList<>(List.of("player_uuid"));
            if (hasHomes) {
                select.add("Homes");
            }
            if (hasNick) {
                select.add("Nickname");
            }
            if (hasBalance) {
                select.add("Balance");
            }

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT " + String.join(", ", select) + " FROM users")) {
                while (rs.next()) {
                    String uuid = rs.getString("player_uuid");
                    if (uuid == null || uuid.isBlank()) {
                        continue;
                    }
                    users++;

                    if (hasHomes) {
                        int[] result = importUserHomes(homeStore, uuid, rs.getString("Homes"));
                        homes += result[0];
                        homesSkipped += result[1];
                    }

                    if (hasNick) {
                        String nick = rs.getString("Nickname");
                        if (nick != null && !nick.isBlank()) {
                            String path = uuid + ".nick";
                            if (!identity.contains(path)) {
                                identity.set(path, nick);
                                nicknames++;
                            }
                        }
                    }

                    if (hasBalance) {
                        double amount = rs.getDouble("Balance");
                        if (!rs.wasNull() && Double.isFinite(amount) && amount > 0
                                && importBalance(economyStore, importLog, uuid, amount)) {
                            balances++;
                        }
                    }
                }
            }

            warps = importWarps(conn, warpStore);
        }

        homeStore.save();
        identity.save();
        warpStore.save();
        importLog.save();
        economyStore.save();

        String summary = "CMI import done (" + users + " users): " + homes + " homes ("
                + homesSkipped + " already existed), " + nicknames + " nicknames, "
                + balances + " balances, " + warps + " warps.";
        report.accept(summary);
    }

    /**
     * Imports one user's {@code Homes} JSON blob.
     *
     * @return {@code [imported, skippedBecauseExisting]}
     */
    private int[] importUserHomes(YamlStore homeStore, String uuid, String json) {
        if (json == null || json.isBlank()) {
            return new int[] {0, 0};
        }
        JsonObject obj;
        try {
            obj = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException ex) {
            return new int[] {0, 0}; // malformed row — skip
        }
        int imported = 0;
        int skipped = 0;
        for (var entry : obj.entrySet()) {
            String name = entry.getKey().toLowerCase(Locale.ROOT);
            String encoded = decode(entry.getValue());
            if (encoded == null) {
                continue;
            }
            String path = uuid + ".homes." + name;
            if (homeStore.contains(path)) {
                skipped++;
                continue;
            }
            homeStore.set(path, encoded);
            imported++;
        }
        return new int[] {imported, skipped};
    }

    /**
     * Imports global warps from CMI's {@code warps} table into the {@code warp} store.
     * A missing table or unrecognised schema simply yields 0 — the rest of the import
     * (homes/nicknames/balances) stands regardless.
     */
    private int importWarps(Connection conn, YamlStore warpStore) {
        try {
            Set<String> cols = columns(conn, "warps");
            String nameCol = cols.contains("name") ? "name" : null;
            String locCol = cols.contains("loc") ? "loc"
                    : cols.contains("location") ? "location" : null;
            if (nameCol == null || locCol == null) {
                return 0;
            }
            int count = 0;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT " + nameCol + ", " + locCol + " FROM warps")) {
                while (rs.next()) {
                    String name = rs.getString(nameCol);
                    String raw = rs.getString(locCol);
                    if (name == null || name.isBlank() || raw == null || raw.isBlank()) {
                        continue;
                    }
                    String encoded = colonToComma(raw);
                    if (encoded == null) {
                        continue;
                    }
                    String path = "warps." + name.toLowerCase(Locale.ROOT);
                    if (!warpStore.contains(path)) {
                        warpStore.set(path, encoded);
                        count++;
                    }
                }
            }
            return count;
        } catch (SQLException ex) {
            return 0; // warps table absent or unreadable
        }
    }

    /**
     * Writes {@code amount} into SEssentials' own {@code economy} store at
     * {@code balances.<uuid>}, so the balance is migrated even while CMI is still the
     * active Vault provider.
     *
     * <p>Idempotency is anchored on the economy store itself: if {@code balances.<uuid>}
     * already exists it is <strong>never overwritten</strong> — that authoritative check
     * (not the marker) is what prevents double-crediting, because it reflects the actual
     * money on disk. If the run crashes before its final save, nothing was persisted and a
     * re-run simply re-imports cleanly. A once-only marker under {@code balance.<uuid>} in
     * the {@code import} store is also recorded for audit.</p>
     *
     * @return {@code true} if the balance was newly migrated
     */
    private boolean importBalance(YamlStore economyStore, YamlStore importLog, String uuid, double amount) {
        UUID id;
        try {
            id = UUID.fromString(uuid);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        String balancePath = BalanceStore.path(id);
        if (economyStore.contains(balancePath)) {
            return false; // existing SEssentials balance — never overwrite
        }
        economyStore.set(balancePath, Double.toString(amount));
        importLog.set("balance." + uuid, "cmi:" + amount);
        return true;
    }

    /** @return the lower-cased column names of {@code table}, or an empty set if it doesn't exist. */
    private static Set<String> columns(Connection conn, String table) throws SQLException {
        Set<String> cols = new HashSet<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                String name = rs.getString("name");
                if (name != null) {
                    cols.add(name.toLowerCase(Locale.ROOT));
                }
            }
        }
        return cols;
    }

    /** CMI value is either {@code ["world:x:y:z:yaw:pitch","icon"]} or a bare string. */
    private static String decode(JsonElement value) {
        String raw;
        if (value.isJsonArray()) {
            JsonArray arr = value.getAsJsonArray();
            if (arr.isEmpty()) {
                return null;
            }
            raw = arr.get(0).getAsString();
        } else if (value.isJsonPrimitive()) {
            raw = value.getAsString();
        } else {
            return null;
        }
        return colonToComma(raw);
    }

    /** {@code "world:x:y:z:yaw:pitch"} → {@code "world,x,y,z,yaw,pitch"}, or {@code null} if malformed. */
    private static String colonToComma(String raw) {
        String[] parts = raw.split(":");
        if (parts.length < 4) {
            return null;
        }
        return String.join(",", parts);
    }
}
