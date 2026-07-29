package dev.iyanz.sessentials.module.items;

import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * {@code /enchant} and {@code /unenchant} (alias {@code /removeenchant}): apply or
 * strip enchantments on the currently held item. Enchanting is "unsafe" — vanilla's
 * max-level and item-target restrictions are bypassed, matching classic essentials
 * behaviour.
 */
@SuppressWarnings("UnstableApiUsage")
final class EnchantCommands {

    /** Enchant levels are clamped to this ceiling to keep tooltips/NBT sane. */
    private static final int MAX_LEVEL = 255;

    private EnchantCommands() {
    }

    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(
                Commands.literal("enchant")
                        .requires(s -> s.getSender().hasPermission("sessentials.enchant"))
                        .then(Commands.argument("enchantment", StringArgumentType.word())
                                .suggests(ItemSuggestions.ENCHANTMENTS)
                                .executes(ctx -> enchant(ctx, 1))
                                .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                        .executes(ctx -> enchant(ctx, IntegerArgumentType.getInteger(ctx, "level")))))
                        .build(),
                "Enchant the item in your hand"));

        plugin.commands(reg -> reg.register(
                Commands.literal("unenchant")
                        .requires(s -> s.getSender().hasPermission("sessentials.unenchant"))
                        .then(Commands.argument("enchantment", StringArgumentType.word())
                                .suggests(ItemSuggestions.ENCHANTMENTS)
                                .executes(EnchantCommands::unenchant))
                        .build(),
                "Remove one or all enchantments from the item in your hand",
                List.of("removeenchant")));
    }

    private static int enchant(CommandContext<CommandSourceStack> ctx, int rawLevel) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        ItemStack hand = heldItemOrError(player);
        if (hand == null) {
            return 0;
        }
        String key = StringArgumentType.getString(ctx, "enchantment");
        Enchantment enchantment = resolveEnchantment(key);
        if (enchantment == null) {
            Msg.err(player, "Unknown enchantment: " + key);
            return 0;
        }

        int level = Math.max(1, Math.min(rawLevel, MAX_LEVEL));
        ItemMeta meta = hand.getItemMeta();
        meta.addEnchant(enchantment, level, true);
        hand.setItemMeta(meta);
        Msg.ok(player, "Enchanted your item with " + Words.titleCase(enchantment.getKey().getKey()) + " " + level + ".");
        return 1;
    }

    private static int unenchant(CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        ItemStack hand = heldItemOrError(player);
        if (hand == null) {
            return 0;
        }
        ItemMeta meta = hand.getItemMeta();
        String key = StringArgumentType.getString(ctx, "enchantment");

        if (key.equalsIgnoreCase("all")) {
            if (!meta.hasEnchants()) {
                Msg.err(player, "Your item has no enchantments.");
                return 0;
            }
            meta.removeEnchantments();
            hand.setItemMeta(meta);
            Msg.ok(player, "Removed all enchantments.");
            return 1;
        }

        Enchantment enchantment = resolveEnchantment(key);
        if (enchantment == null) {
            Msg.err(player, "Unknown enchantment: " + key);
            return 0;
        }
        if (!meta.removeEnchant(enchantment)) {
            Msg.err(player, "Your item does not have that enchantment.");
            return 0;
        }
        hand.setItemMeta(meta);
        Msg.ok(player, "Removed " + Words.titleCase(enchantment.getKey().getKey()) + ".");
        return 1;
    }

    private static ItemStack heldItemOrError(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            Msg.err(player, "You are not holding an item.");
            return null;
        }
        return hand;
    }

    /**
     * Resolves an enchantment from a plain key ({@code "sharpness"}) or a namespaced
     * one ({@code "minecraft:sharpness"}), case-insensitively.
     *
     * @param input the typed enchantment argument
     * @return the matching enchantment, or {@code null} if none is registered under that key
     */
    private static Enchantment resolveEnchantment(String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        NamespacedKey key = lower.indexOf(':') >= 0 ? NamespacedKey.fromString(lower) : NamespacedKey.minecraft(lower);
        return key == null ? null : ItemSuggestions.ENCHANTMENT_REGISTRY.get(key);
    }
}
