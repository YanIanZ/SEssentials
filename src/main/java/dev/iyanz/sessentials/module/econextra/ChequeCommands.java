package dev.iyanz.sessentials.module.econextra;

import java.util.Map;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.economy.EconomyHook;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * {@code /cheque <amount>}: withdraws {@code amount} (shorthand accepted, e.g.
 * {@code 1.5m}) from the sender's balance and hands them a redeemable paper cheque
 * for it (see {@link ChequeItems}). Right-clicking the cheque later deposits the
 * amount back and consumes the item (see {@link ChequeListener}).
 */
@SuppressWarnings("UnstableApiUsage")
final class ChequeCommands {

    private ChequeCommands() {
    }

    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("cheque")
                .requires(s -> s.getSender().hasPermission("sessentials.cheque"))
                .then(Commands.argument("amount", StringArgumentType.word())
                        .executes(ctx -> cheque(plugin, ctx)))
                .build(), "Withdraw money into a redeemable cheque"));
    }

    private static int cheque(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        if (!plugin.economy().available()) {
            Msg.err(player, "No economy provider found.");
            return 0;
        }
        double amount = Amounts.parse(StringArgumentType.getString(ctx, "amount"));
        if (Double.isNaN(amount)) {
            Msg.err(player, "Invalid amount.");
            return 0;
        }
        EconomyHook eco = plugin.economy();
        if (!eco.has(player.getUniqueId(), amount)) {
            Msg.err(player, "You don't have that much.");
            return 0;
        }
        if (!eco.withdraw(player.getUniqueId(), amount)) {
            Msg.err(player, "Withdrawal failed.");
            return 0;
        }
        String formatted = eco.format(amount);
        ItemStack cheque = ChequeItems.create(plugin, amount, formatted);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(cheque);
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        Msg.value(player, "Cheque created for:", formatted);
        return 1;
    }
}
