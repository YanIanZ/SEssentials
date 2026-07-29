package dev.iyanz.sessentials.module.items;

import java.util.List;
import java.util.Map;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * {@code /give} (aliases {@code i}, {@code item}) and {@code /more}: put items into a
 * player's inventory and top up the currently held stack.
 *
 * <p>{@code give}, {@code i} and {@code item} are the exact same command tree — when
 * no target player is supplied, the item goes to the sender (the common case for
 * {@code /i <material>}), while a console sender must always name a player, since it
 * has no inventory of its own.</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class GiveMoreCommands {

    /** Highest amount a single {@code /give} may hand out (64 full stacks worth). */
    private static final int MAX_GIVE_AMOUNT = 64 * 36;

    private GiveMoreCommands() {
    }

    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(
                Commands.literal("give")
                        .requires(s -> s.getSender().hasPermission("sessentials.give"))
                        .then(Commands.argument("material", StringArgumentType.word())
                                .suggests(ItemSuggestions.MATERIALS)
                                .executes(ctx -> give(plugin, ctx, 1, null))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> give(plugin, ctx,
                                                IntegerArgumentType.getInteger(ctx, "amount"), null))
                                        .then(Commands.argument("player", StringArgumentType.word())
                                                .suggests(Cmds.PLAYERS)
                                                .executes(ctx -> give(plugin, ctx,
                                                        IntegerArgumentType.getInteger(ctx, "amount"),
                                                        StringArgumentType.getString(ctx, "player"))))))
                        .build(),
                "Give yourself or a player an item",
                List.of("i", "item")));

        plugin.commands(reg -> reg.register(
                Commands.literal("more")
                        .requires(s -> s.getSender().hasPermission("sessentials.more"))
                        .executes(Cmds.playerExec(GiveMoreCommands::more))
                        .build(),
                "Fill the item in your hand to a full stack"));
    }

    private static int give(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx, int rawAmount, String targetName) {
        CommandSender sender = ctx.getSource().getSender();

        Material material = Material.matchMaterial(StringArgumentType.getString(ctx, "material"));
        if (material == null || !material.isItem()) {
            Msg.err(sender, "That is not a valid item.");
            return 0;
        }

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

        int amount = Math.max(1, Math.min(rawAmount, MAX_GIVE_AMOUNT));
        String itemName = Words.titleCase(material.name());
        Player finalTarget = target;
        boolean toSelf = finalTarget.equals(sender);

        if (!toSelf) {
            Msg.ok(sender, "Gave " + finalTarget.getName() + " " + amount + "x " + itemName + ".");
        }
        TargetActions.run(plugin, sender, finalTarget, () -> {
            Map<Integer, ItemStack> overflow = finalTarget.getInventory().addItem(new ItemStack(material, amount));
            for (ItemStack leftover : overflow.values()) {
                finalTarget.getWorld().dropItemNaturally(finalTarget.getLocation(), leftover);
            }
            Msg.ok(finalTarget, "You received " + amount + "x " + itemName + ".");
        });
        return 1;
    }

    private static void more(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            Msg.err(player, "You are not holding an item.");
            return;
        }
        hand.setAmount(hand.getMaxStackSize());
        Msg.ok(player, "Stack filled.");
    }
}
