package dev.iyanz.sessentials.module.itemadmin;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Simple held-item meta toggles and setters:
 * <ul>
 *   <li>{@code /hideflags} — toggles every {@link ItemFlag} (hide attributes,
 *       enchants, dye, trim, etc.) on the held item; if any flag is present, all
 *       are removed, otherwise all are added</li>
 *   <li>{@code /unbreakable} — toggles the held item's unbreakable state</li>
 *   <li>{@code /itemcmdata &lt;n&gt;} — sets the held item's custom model data
 *       to {@code n}, or clears it when {@code n &lt;= 0}</li>
 * </ul>
 * All player-only; the executor already runs on the sender's region thread.
 */
@SuppressWarnings("UnstableApiUsage")
final class MetaToggleCommands {

    private MetaToggleCommands() {
    }

    /**
     * Registers {@code /hideflags}, {@code /unbreakable} and {@code /itemcmdata}.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(
                Commands.literal("hideflags")
                        .requires(s -> s.getSender().hasPermission("sessentials.hideflags"))
                        .executes(MetaToggleCommands::toggleFlags)
                        .build(),
                "Toggle all item flags on the item in your hand"));

        plugin.commands(reg -> reg.register(
                Commands.literal("unbreakable")
                        .requires(s -> s.getSender().hasPermission("sessentials.unbreakable"))
                        .executes(MetaToggleCommands::toggleUnbreakable)
                        .build(),
                "Toggle unbreakability of the item in your hand"));

        plugin.commands(reg -> reg.register(
                Commands.literal("itemcmdata")
                        .requires(s -> s.getSender().hasPermission("sessentials.itemcmdata"))
                        .then(Commands.argument("value", IntegerArgumentType.integer())
                                .executes(MetaToggleCommands::customModelData))
                        .build(),
                "Set (or clear with 0) the held item's custom model data"));
    }

    private static int toggleFlags(CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        ItemStack hand = Held.mainHandOrError(player);
        if (hand == null) {
            return 0;
        }
        ItemMeta meta = hand.getItemMeta();
        boolean hide = meta.getItemFlags().isEmpty();
        if (hide) {
            meta.addItemFlags(ItemFlag.values());
        } else {
            meta.removeItemFlags(ItemFlag.values());
        }
        hand.setItemMeta(meta);
        Msg.ok(player, hide ? "Item flags hidden." : "Item flags shown.");
        return 1;
    }

    private static int toggleUnbreakable(CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        ItemStack hand = Held.mainHandOrError(player);
        if (hand == null) {
            return 0;
        }
        ItemMeta meta = hand.getItemMeta();
        boolean unbreakable = !meta.isUnbreakable();
        meta.setUnbreakable(unbreakable);
        hand.setItemMeta(meta);
        Msg.ok(player, unbreakable ? "Item is now unbreakable." : "Item is breakable again.");
        return 1;
    }

    @SuppressWarnings("deprecation") // setCustomModelData(Integer): brief-mandated simple int API
    private static int customModelData(CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        ItemStack hand = Held.mainHandOrError(player);
        if (hand == null) {
            return 0;
        }
        int value = IntegerArgumentType.getInteger(ctx, "value");
        ItemMeta meta = hand.getItemMeta();
        if (value <= 0) {
            meta.setCustomModelData(null);
            hand.setItemMeta(meta);
            Msg.ok(player, "Custom model data cleared.");
        } else {
            meta.setCustomModelData(value);
            hand.setItemMeta(meta);
            Msg.value(player, "Custom model data set to:", String.valueOf(value));
        }
        return 1;
    }
}
