package dev.iyanz.sessentials.module.disableenchant;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import dev.iyanz.sessentials.SEssentialsPlugin;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Prevents a configurable list of enchantments from ever landing on an item, whether
 * offered at an enchanting table or produced by combining an item with an enchanted
 * book on an anvil.
 *
 * <p>Reads {@code disable-enchant.enabled} (default {@code false}) and {@code
 * disable-enchant.blocked} (a string list of enchantment keys, e.g. {@code
 * ["mending", "sharpness"]}) fresh on every event, so a config reload takes effect
 * immediately. Enchantments are matched by their plain registry key ({@link
 * Enchantment#getKey()}'s {@link org.bukkit.NamespacedKey#getKey()}), case-insensitively,
 * so both {@code "mending"} and {@code "minecraft:mending"}-style keys in config compare
 * the same way.</p>
 *
 * <p>The gate is silent by design: a blocked enchantment is simply dropped (or the
 * whole interaction cancelled) with no message to the player, matching a "the option
 * never existed" feel rather than an explicit denial.</p>
 */
public final class EnchantGateListener implements Listener {

    private final SEssentialsPlugin plugin;

    /**
     * @param plugin the owning plugin, used for its live {@code disable-enchant.*} config
     */
    public EnchantGateListener(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Strips any blocked enchantment out of an enchanting-table result; if that leaves
     * nothing to enchant with, the whole event is cancelled instead of silently
     * charging the player for an empty result.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        if (!plugin.getConfig().getBoolean("disable-enchant.enabled", false)) {
            return;
        }
        Set<String> blocked = blockedKeys();
        if (blocked.isEmpty()) {
            return;
        }

        Map<Enchantment, Integer> enchantsToAdd = event.getEnchantsToAdd();
        enchantsToAdd.keySet().removeIf(enchantment -> isBlocked(enchantment, blocked));
        if (enchantsToAdd.isEmpty()) {
            event.setCancelled(true);
        }
    }

    /** Blocks an anvil combination whose resulting item would carry a blocked enchantment. */
    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!plugin.getConfig().getBoolean("disable-enchant.enabled", false)) {
            return;
        }
        Set<String> blocked = blockedKeys();
        if (blocked.isEmpty()) {
            return;
        }

        if (hasBlockedEnchant(event.getResult(), blocked)) {
            event.setResult(null);
        }
    }

    /** @return the configured blocked enchantment keys, lower-cased and never {@code null}. */
    private Set<String> blockedKeys() {
        List<String> raw = plugin.getConfig().getStringList("disable-enchant.blocked");
        if (raw.isEmpty()) {
            return Set.of();
        }
        Set<String> blocked = new HashSet<>(raw.size());
        for (String key : raw) {
            if (key != null && !key.isBlank()) {
                blocked.add(key.toLowerCase(Locale.ROOT));
            }
        }
        return blocked;
    }

    /** @return {@code true} if {@code enchantment}'s plain key is in {@code blocked}. */
    private static boolean isBlocked(Enchantment enchantment, Set<String> blocked) {
        return blocked.contains(enchantment.getKey().getKey().toLowerCase(Locale.ROOT));
    }

    /**
     * @return {@code true} if {@code item} carries any enchantment (direct, or stored
     *         on an enchanted book) whose key is in {@code blocked}
     */
    private static boolean hasBlockedEnchant(ItemStack item, Set<String> blocked) {
        if (item == null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        Map<Enchantment, Integer> enchants = meta instanceof EnchantmentStorageMeta storageMeta
                ? storageMeta.getStoredEnchants()
                : meta.getEnchants();
        for (Enchantment enchantment : enchants.keySet()) {
            if (isBlocked(enchantment, blocked)) {
                return true;
            }
        }
        return false;
    }
}
