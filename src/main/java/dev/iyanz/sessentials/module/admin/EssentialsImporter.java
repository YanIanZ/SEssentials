package dev.iyanz.sessentials.module.admin;

import java.io.File;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.store.YamlStore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Imports player data from EssentialsX's YAML files:
 * <ul>
 *   <li><b>Homes</b> — {@code userdata/<uuid>.yml} {@code homes.<name>} sections
 *       → {@code <uuid>.homes.<name>} in the {@code home} store;</li>
 *   <li><b>Nicknames</b> — {@code userdata/<uuid>.yml} {@code nickname}
 *       → {@code <uuid>.nick} in the {@code identity} store (legacy colour codes are
 *       fine — the nick module converts them on render);</li>
 *   <li><b>Balances</b> — {@code userdata/<uuid>.yml} {@code money} → deposited into the
 *       Vault economy (skipped when no economy is hooked). A marker under
 *       {@code balance.<uuid>} in the {@code import} store guarantees a player's balance
 *       is only ever deposited once across re-runs;</li>
 *   <li><b>Warps</b> — {@code warps/<name>.yml} → {@code warps.<name>} in the
 *       {@code warp} store.</li>
 * </ul>
 * Locations are converted to SEssentials' {@code world,x,y,z,yaw,pitch} format, absent
 * fields are skipped cleanly, and existing SEssentials entries are never overwritten.
 * Runs entirely off the main thread.
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
        UserCounts users = importUserdata(new File(essentialsDir, "userdata"));
        int warps = importWarps(new File(essentialsDir, "warps"));
        String summary = "Essentials import done: " + users.homes() + " homes, "
                + users.nicknames() + " nicknames, " + users.balances() + " balances, "
                + warps + " warps.";
        if (users.balanceSeen() && !plugin.economy().available()) {
            summary += " Economy unavailable — balances skipped.";
        }
        report.accept(summary);
    }

    /** Per-category results of the {@code userdata} pass. */
    private record UserCounts(int homes, int nicknames, int balances, boolean balanceSeen) {
    }

    /** Single pass over {@code userdata/<uuid>.yml}: homes, nickname and money per player. */
    private UserCounts importUserdata(File userdata) {
        File[] files = userdata.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) {
            return new UserCounts(0, 0, 0, false);
        }
        YamlStore homeStore = plugin.stores().get("home");
        YamlStore identity = plugin.stores().get("identity");
        YamlStore importLog = plugin.stores().get("import");
        boolean economy = plugin.economy().available();

        int homes = 0;
        int nicknames = 0;
        int balances = 0;
        boolean balanceSeen = false;

        for (File file : files) {
            String uuid = file.getName().substring(0, file.getName().length() - 4);
            try {
                UUID.fromString(uuid);
            } catch (IllegalArgumentException ex) {
                continue; // username-era file — can't key our per-UUID stores
            }
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

            ConfigurationSection homeSec = cfg.getConfigurationSection("homes");
            if (homeSec != null) {
                for (String name : homeSec.getKeys(false)) {
                    String encoded = encode(homeSec.getConfigurationSection(name));
                    if (encoded == null) {
                        continue;
                    }
                    String path = uuid + ".homes." + name.toLowerCase(Locale.ROOT);
                    if (!homeStore.contains(path)) {
                        homeStore.set(path, encoded);
                        homes++;
                    }
                }
            }

            String nick = cfg.getString("nickname");
            if (nick != null && !nick.isBlank()) {
                String path = uuid + ".nick";
                if (!identity.contains(path)) {
                    identity.set(path, nick);
                    nicknames++;
                }
            }

            String money = cfg.getString("money");
            if (money != null && !money.isBlank()) {
                double amount;
                try {
                    amount = Double.parseDouble(money.trim());
                } catch (NumberFormatException ex) {
                    amount = -1; // malformed — skip below
                }
                if (Double.isFinite(amount) && amount > 0) {
                    balanceSeen = true;
                    if (economy && depositOnce(importLog, uuid, amount)) {
                        balances++;
                    }
                }
            }
        }
        homeStore.save();
        identity.save();
        importLog.save();
        return new UserCounts(homes, nicknames, balances, balanceSeen);
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

    /**
     * Deposits {@code amount} into the player's Vault balance exactly once: a marker under
     * {@code balance.<uuid>} in the {@code import} store blocks double-deposits across
     * repeated imports (from any source).
     */
    private boolean depositOnce(YamlStore importLog, String uuid, double amount) {
        String marker = "balance." + uuid;
        if (importLog.contains(marker)) {
            return false;
        }
        if (!plugin.economy().deposit(UUID.fromString(uuid), amount)) {
            return false;
        }
        importLog.set(marker, "essentials:" + amount);
        return true;
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
