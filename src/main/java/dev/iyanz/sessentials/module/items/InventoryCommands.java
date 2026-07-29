package dev.iyanz.sessentials.module.items;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * {@code /clear} (aliases {@code /ci}, {@code /clearinventory}) and {@code /condense}:
 * bulk inventory maintenance.
 */
@SuppressWarnings("UnstableApiUsage")
final class InventoryCommands {

    private InventoryCommands() {
    }

    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(
                Commands.literal("clear")
                        .requires(s -> s.getSender().hasPermission("sessentials.clear"))
                        .executes(ctx -> clear(plugin, ctx, null))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(Cmds.PLAYERS)
                                .requires(s -> s.getSender().hasPermission("sessentials.clear.others"))
                                .executes(ctx -> clear(plugin, ctx, StringArgumentType.getString(ctx, "player"))))
                        .build(),
                "Clear your (or a player's) inventory",
                List.of("ci", "clearinventory")));

        plugin.commands(reg -> reg.register(
                Commands.literal("condense")
                        .requires(s -> s.getSender().hasPermission("sessentials.condense"))
                        .executes(Cmds.playerExec(InventoryCommands::condense))
                        .build(),
                "Compact your inventory's stacks into full ones"));
    }

    private static int clear(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx, String targetName) {
        CommandSender sender = ctx.getSource().getSender();

        Player target;
        if (targetName != null) {
            target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                Msg.err(sender, targetName + " is not online.");
                return 0;
            }
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            Msg.err(sender, "Console must specify a player.");
            return 0;
        }

        Player finalTarget = target;
        boolean toSelf = finalTarget.equals(sender);
        if (!toSelf) {
            Msg.ok(sender, "Cleared " + finalTarget.getName() + "'s inventory.");
        }
        TargetActions.run(plugin, sender, finalTarget, () -> {
            finalTarget.getInventory().clear();
            Msg.ok(finalTarget, "Your inventory was cleared.");
        });
        return 1;
    }

    private static void condense(Player player) {
        Inventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getStorageContents();

        List<ItemStack> representatives = new ArrayList<>();
        List<Integer> totals = new ArrayList<>();
        for (ItemStack stack : contents) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            int bucket = -1;
            for (int i = 0; i < representatives.size(); i++) {
                if (representatives.get(i).isSimilar(stack)) {
                    bucket = i;
                    break;
                }
            }
            if (bucket == -1) {
                representatives.add(stack);
                totals.add(stack.getAmount());
            } else {
                totals.set(bucket, totals.get(bucket) + stack.getAmount());
            }
        }

        ItemStack[] rebuilt = new ItemStack[contents.length];
        int slot = 0;
        for (int i = 0; i < representatives.size() && slot < rebuilt.length; i++) {
            ItemStack sample = representatives.get(i);
            int remaining = totals.get(i);
            int maxStack = sample.getMaxStackSize();
            while (remaining > 0 && slot < rebuilt.length) {
                int amount = Math.min(maxStack, remaining);
                ItemStack piece = sample.clone();
                piece.setAmount(amount);
                rebuilt[slot++] = piece;
                remaining -= amount;
            }
        }

        inventory.setStorageContents(rebuilt);
        Msg.ok(player, "Inventory condensed.");
    }
}
