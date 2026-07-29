package dev.iyanz.sessentials.module.nbt;

import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Registers {@code /itemnbt} (alias {@code /iteminfo}): a read-only report about the
 * item currently in the sender's main hand — material, amount, plain display name,
 * enchantments, durability/damage (if applicable) and any custom
 * {@link org.bukkit.persistence.PersistentDataContainer} keys.
 *
 * <p>Only ever inspects the sender's own held item, so it runs safely on their own
 * region thread with no scheduler hop, and never mutates the item.</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class ItemNbtCommand {

    private ItemNbtCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            var node = Commands.literal("itemnbt")
                    .requires(s -> s.getSender().hasPermission("sessentials.nbt"))
                    .executes(Cmds.playerExec(ItemNbtCommand::report))
                    .build();
            reg.register(node, "Show info about the item in your hand", List.of("iteminfo"));
        });
    }

    private static void report(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.isEmpty()) {
            Msg.err(player, "You are not holding anything.");
            return;
        }

        ItemMeta meta = item.getItemMeta();

        Msg.value(player, "Item:", NbtFormat.titleCase(item.getType().name()));
        Msg.value(player, "Amount:", String.valueOf(item.getAmount()));

        String displayName = meta != null && meta.hasDisplayName()
                ? PlainTextComponentSerializer.plainText().serialize(meta.displayName())
                : "none";
        Msg.value(player, "Display name:", displayName);

        if (meta instanceof Damageable damageable) {
            Msg.value(player, "Damage:", damageable.getDamage() + " / " + damageable.getMaxDamage());
        }

        reportEnchantments(player, item.getEnchantments());
        reportPdcKeys(player, meta);
    }

    private static void reportEnchantments(Player player, Map<Enchantment, Integer> enchantments) {
        if (enchantments.isEmpty()) {
            Msg.value(player, "Enchantments:", "none");
            return;
        }
        Msg.info(player, enchantments.size() + " enchantment(s):");
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Msg.value(player, "  " + NbtFormat.titleCase(entry.getKey().getKey().getKey()) + ":", "Lvl " + entry.getValue());
        }
    }

    private static void reportPdcKeys(Player player, ItemMeta meta) {
        Set<NamespacedKey> keys = meta != null ? meta.getPersistentDataContainer().getKeys() : Set.of();
        if (keys.isEmpty()) {
            Msg.value(player, "PDC keys:", "none");
            return;
        }
        Msg.info(player, keys.size() + " custom data key(s):");
        for (NamespacedKey key : keys) {
            Msg.value(player, "  -", key.toString());
        }
    }
}
