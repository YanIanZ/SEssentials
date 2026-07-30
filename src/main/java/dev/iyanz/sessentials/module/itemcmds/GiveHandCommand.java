package dev.iyanz.sessentials.module.itemcmds;

import java.util.Locale;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * {@code /givehand <player>}: gives the target player a copy of the item currently
 * in the sender's main hand. The sender's item is untouched (the target receives a
 * clone), so this is an admin "duplicate to player" tool rather than a trade.
 *
 * <p>Folia: the held-item read happens in the executor (sender's region thread);
 * the target's inventory mutation is hopped onto the target's region thread, with
 * any overflow dropped naturally at the target's feet.</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class GiveHandCommand {

    private GiveHandCommand() {
    }

    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(
                Commands.literal("givehand")
                        .requires(s -> s.getSender().hasPermission("sessentials.givehand"))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(Cmds.PLAYERS)
                                .executes(ctx -> give(plugin, ctx)))
                        .build(),
                "Give a copy of your held item to a player"));
    }

    /**
     * Clones the sender's held item and delivers it to the named target.
     *
     * @param plugin the owning plugin (for the region-thread hop)
     * @param ctx    the command context
     * @return 1 on success, 0 on error
     */
    private static int give(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        Player sender = Cmds.player(ctx);
        if (sender == null) {
            return 0;
        }
        ItemStack hand = sender.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            Msg.err(sender, "You are not holding an item.");
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Msg.err(sender, name + " is not online.");
            return 0;
        }

        ItemStack copy = hand.clone();
        String itemName = copy.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        Schedulers.entity(plugin, target, () -> {
            for (ItemStack overflow : target.getInventory().addItem(copy).values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), overflow);
            }
            Msg.ok(target, "You received " + copy.getAmount() + "x " + itemName + " from " + sender.getName() + ".");
            Msg.ok(sender, "Gave " + copy.getAmount() + "x " + itemName + " to " + target.getName() + ".");
        });
        return 1;
    }
}
