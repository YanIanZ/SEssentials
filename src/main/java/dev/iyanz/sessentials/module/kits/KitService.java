package dev.iyanz.sessentials.module.kits;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.iyanz.sessentials.store.YamlStore;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Persistence and cooldown bookkeeping for kits, backed by two {@link YamlStore}s:
 * {@code kits.yml} (kit definitions) and {@code kitcooldowns.yml} (per-player,
 * per-kit next-available timestamps).
 *
 * <p>Kit items are native Bukkit {@link ItemStack}s (which are
 * {@code ConfigurationSerializable}), stored as a list under
 * {@code kits.<name>.items}; each kit's cooldown (seconds) is stored under
 * {@code kits.<name>.cooldown}. Cooldown state is stored under
 * {@code <uuid>.<kit>} as the epoch-millisecond timestamp the kit becomes
 * available again for that player.</p>
 */
public final class KitService {

    private static final String DEFAULT_KIT_NAME = "starter";

    private final YamlStore kits;
    private final YamlStore cooldowns;
    private final int defaultCooldownSeconds;

    /**
     * @param kits                   the {@code kits} data store (kit definitions)
     * @param cooldowns              the {@code kitcooldowns} data store (per-player cooldown state)
     * @param defaultCooldownSeconds the fallback cooldown (seconds) for kits without an explicit one,
     *                               read from config {@code kits.default-cooldown}
     */
    public KitService(YamlStore kits, YamlStore cooldowns, int defaultCooldownSeconds) {
        this.kits = kits;
        this.cooldowns = cooldowns;
        this.defaultCooldownSeconds = defaultCooldownSeconds;
    }

    /** @return the configured fallback cooldown (seconds) for kits without an explicit one. */
    public int defaultCooldownSeconds() {
        return defaultCooldownSeconds;
    }

    /**
     * Creates a small demonstrative {@code starter} kit if no kits have been defined
     * yet (i.e. this is the plugin's first run with an empty kit store). No-op
     * otherwise.
     */
    public void ensureDefaults() {
        if (!kits.keys("kits").isEmpty()) {
            return;
        }
        List<ItemStack> starter = List.of(
                new ItemStack(Material.STONE_SWORD),
                new ItemStack(Material.BREAD, 8),
                new ItemStack(Material.OAK_LOG, 16));
        save(DEFAULT_KIT_NAME, starter, defaultCooldownSeconds);
    }

    /** @return the names of every defined kit, alphabetically sorted. */
    public List<String> names() {
        List<String> names = new ArrayList<>(kits.keys("kits"));
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    /** @return whether a kit named {@code name} exists. */
    public boolean exists(String name) {
        return kits.contains("kits." + name);
    }

    /**
     * Loads a kit definition.
     *
     * @param name the kit name
     * @return the kit, or empty if no kit named {@code name} is defined
     */
    public Optional<Kit> get(String name) {
        if (!exists(name)) {
            return Optional.empty();
        }
        int cooldown = kits.config().getInt("kits." + name + ".cooldown", defaultCooldownSeconds);
        List<?> raw = kits.config().getList("kits." + name + ".items");
        List<ItemStack> items = new ArrayList<>();
        if (raw != null) {
            for (Object entry : raw) {
                if (entry instanceof ItemStack stack) {
                    items.add(stack);
                }
            }
        }
        return Optional.of(new Kit(name, cooldown, items));
    }

    /**
     * Saves (creating or overwriting) a kit's items and cooldown, then persists to disk.
     *
     * @param name            the kit name
     * @param items           the items granted by this kit
     * @param cooldownSeconds seconds a player must wait between uses
     */
    public void save(String name, List<ItemStack> items, int cooldownSeconds) {
        kits.set("kits." + name + ".items", items);
        kits.set("kits." + name + ".cooldown", cooldownSeconds);
        kits.save();
    }

    /**
     * Removes a kit definition entirely, then persists to disk. No-op if the kit
     * does not exist.
     *
     * @param name the kit name
     */
    public void delete(String name) {
        kits.remove("kits." + name);
        kits.save();
    }

    /**
     * @param player the player's UUID
     * @param name   the kit name
     * @return the remaining cooldown in whole seconds (rounded up), or {@code 0} if
     *         the kit is available right now
     */
    public long remainingSeconds(UUID player, String name) {
        long nextAvailable = cooldowns.config().getLong(player + "." + name, 0L);
        long remainingMillis = nextAvailable - System.currentTimeMillis();
        return remainingMillis <= 0 ? 0L : (remainingMillis + 999) / 1000;
    }

    /**
     * Starts (or restarts) a player's cooldown for a kit, then persists to disk.
     *
     * @param player          the player's UUID
     * @param name            the kit name
     * @param cooldownSeconds seconds until the kit becomes available again
     */
    public void startCooldown(UUID player, String name, int cooldownSeconds) {
        long nextAvailable = System.currentTimeMillis() + (cooldownSeconds * 1000L);
        cooldowns.set(player + "." + name, nextAvailable);
        cooldowns.save();
    }

    /**
     * Grants (defensive copies of) {@code items} to {@code player}'s inventory,
     * dropping any overflow naturally at their feet. Shared by both the
     * {@code /kit} command and the kits GUI so item-granting lives in one place.
     *
     * @param player the recipient
     * @param items  the items to grant; each is cloned, so the caller's list (the
     *               kit's cached definition) is left untouched
     */
    public void grant(Player player, List<ItemStack> items) {
        ItemStack[] copies = new ItemStack[items.size()];
        for (int i = 0; i < items.size(); i++) {
            copies[i] = items.get(i).clone();
        }
        var overflow = player.getInventory().addItem(copies);
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    /** @return the dynamic per-kit permission node, {@code "sessentials.kit.<name>"}. */
    public static String permission(String name) {
        return "sessentials.kit." + name;
    }

    /** Formats whole seconds as a compact {@code "1h 5m 12s"}-style duration. */
    public static String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        StringBuilder out = new StringBuilder();
        if (hours > 0) {
            out.append(hours).append("h ");
        }
        if (hours > 0 || minutes > 0) {
            out.append(minutes).append("m ");
        }
        out.append(seconds).append("s");
        return out.toString();
    }
}
