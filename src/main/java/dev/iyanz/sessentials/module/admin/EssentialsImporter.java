package dev.iyanz.sessentials.module.admin;

import java.io.File;
import java.util.Locale;
import java.util.function.Consumer;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.store.YamlStore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Imports homes and warps from EssentialsX's YAML data:
 * {@code plugins/Essentials/userdata/<uuid>.yml} ({@code homes.<name>}) and
 * {@code plugins/Essentials/warps/<name>.yml}. Locations are converted to SEssentials'
 * {@code world,x,y,z,yaw,pitch} format; existing entries are never overwritten.
 */
final class EssentialsImporter {

    private final SEssentialsPlugin plugin;

    EssentialsImporter(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    void run(File essentialsDir, Consumer<String> report) {
        if (!essentialsDir.isDirectory()) {
            report.accept("Essentials folder not found at " + essentialsDir.getPath() + ".");
            return;
        }
        int homes = importHomes(new File(essentialsDir, "userdata"));
        int warps = importWarps(new File(essentialsDir, "warps"));
        report.accept("Essentials import done: " + homes + " homes, " + warps + " warps.");
    }

    private int importHomes(File userdata) {
        File[] files = userdata.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) {
            return 0;
        }
        YamlStore store = plugin.stores().get("home");
        int count = 0;
        for (File file : files) {
            String uuid = file.getName().substring(0, file.getName().length() - 4);
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection homes = cfg.getConfigurationSection("homes");
            if (homes == null) {
                continue;
            }
            for (String name : homes.getKeys(false)) {
                String encoded = encode(homes.getConfigurationSection(name));
                if (encoded == null) {
                    continue;
                }
                String path = uuid + ".homes." + name.toLowerCase(Locale.ROOT);
                if (!store.contains(path)) {
                    store.set(path, encoded);
                    count++;
                }
            }
        }
        store.save();
        return count;
    }

    private int importWarps(File warpsDir) {
        File[] files = warpsDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) {
            return 0;
        }
        YamlStore store = plugin.stores().get("warp");
        int count = 0;
        for (File file : files) {
            String name = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            String encoded = encode(cfg);
            if (encoded == null) {
                continue;
            }
            String path = "warps." + name;
            if (!store.contains(path)) {
                store.set(path, encoded);
                count++;
            }
        }
        store.save();
        return count;
    }

    /** Reads world-name/x/y/z/yaw/pitch from an Essentials location section. */
    private static String encode(ConfigurationSection sec) {
        if (sec == null || !sec.contains("x")) {
            return null;
        }
        String world = sec.getString("world-name", sec.getString("world"));
        if (world == null) {
            return null;
        }
        double x = sec.getDouble("x");
        double y = sec.getDouble("y");
        double z = sec.getDouble("z");
        double yaw = sec.getDouble("yaw");
        double pitch = sec.getDouble("pitch");
        return world + "," + x + "," + y + "," + z + "," + yaw + "," + pitch;
    }
}
