package dev.iyanz.sessentials.module.itemcmds;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

/**
 * {@code /enchantbook <enchant> [level]}: gives the sender a freshly created
 * enchanted book whose {@link EnchantmentStorageMeta stored enchantment} is the
 * chosen one, at the chosen level (default 1). Levels are unrestricted by vanilla
 * caps, matching the {@code /enchant} command's "unsafe" behaviour, but clamped to
 * a sane ceiling.
 */
@SuppressWarnings("UnstableApiUsage")
final class EnchantBookCommand {

    /** Stored-enchant levels are clamped to this ceiling to keep item NBT sane. */
    private static final int MAX_LEVEL = 255;

    private EnchantBookCommand() {
    }

    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(
                Commands.literal("enchantbook")
                        .requires(s -> s.getSender().hasPermission("sessentials.enchantbook"))
                        .then(Commands.argument("enchantment", StringArgumentType.word())
                                .suggests(Enchants.SUGGESTIONS)
                                .executes(ctx -> give(ctx, 1))
                                .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                        .executes(ctx -> give(ctx, IntegerArgumentType.getInteger(ctx, "level")))))
                        .build(),
                "Get an enchanted book with the given stored enchantment"));
    }

    /**
     * Creates the book and hands it to the sender. Runs on the sender's region
     * thread (the command executor), so inventory access is safe as-is.
     *
     * @param ctx      the command context
     * @param rawLevel the requested level (clamped to {@code 1..255})
     * @return 1 on success, 0 on error
     */
    private static int give(CommandContext<CommandSourceStack> ctx, int rawLevel) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        String input = StringArgumentType.getString(ctx, "enchantment");
        Enchantment enchantment = Enchants.resolve(input);
        if (enchantment == null) {
            Msg.err(player, "Unknown enchantment: " + input);
            return 0;
        }

        int level = Math.min(rawLevel, MAX_LEVEL);
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(enchantment, level, true);
        book.setItemMeta(meta);

        for (ItemStack overflow : player.getInventory().addItem(book).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), overflow);
        }
        Msg.ok(player, "Received an enchanted book: " + Enchants.displayName(enchantment) + " " + level + ".");
        return 1;
    }
}
