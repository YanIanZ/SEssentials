package dev.iyanz.sessentials.module.admin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;
import java.util.function.Consumer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.store.YamlStore;

/**
 * Imports player homes from CMI's {@code cmi.sqlite.db}. CMI stores homes in the
 * {@code users.Homes} column as JSON: {@code {"HomeName":["world:x:y:z:yaw:pitch","icon"]}}.
 * Each home is converted to SEssentials' {@code world,x,y,z,yaw,pitch} format and
 * written under {@code <uuid>.homes.<name>} in the {@code home} store, skipping names
 * that already exist.
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

        YamlStore homes = plugin.stores().get("home");
        int users = 0;
        int imported = 0;
        int skipped = 0;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT player_uuid, Homes FROM users WHERE Homes IS NOT NULL AND Homes != ''")) {
            while (rs.next()) {
                String uuid = rs.getString("player_uuid");
                String json = rs.getString("Homes");
                if (uuid == null || json == null || json.isBlank()) {
                    continue;
                }
                users++;
                JsonObject obj;
                try {
                    obj = JsonParser.parseString(json).getAsJsonObject();
                } catch (RuntimeException ex) {
                    continue; // malformed row — skip
                }
                for (var entry : obj.entrySet()) {
                    String name = entry.getKey().toLowerCase(Locale.ROOT);
                    String encoded = decode(entry.getValue());
                    if (encoded == null) {
                        continue;
                    }
                    String path = uuid + ".homes." + name;
                    if (homes.contains(path)) {
                        skipped++;
                        continue;
                    }
                    homes.set(path, encoded);
                    imported++;
                }
            }
        }
        homes.save();
        report.accept("CMI import done: " + imported + " homes from " + users
                + " players (" + skipped + " already existed).");
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
        // "world:x:y:z:yaw:pitch" → "world,x,y,z,yaw,pitch"
        String[] parts = raw.split(":");
        if (parts.length < 4) {
            return null;
        }
        return String.join(",", parts);
    }
}
