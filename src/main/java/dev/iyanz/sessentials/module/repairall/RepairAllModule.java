package dev.iyanz.sessentials.module.repairall;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;

/**
 * Repair-all module: {@code /repairall} restores every damageable item in a player's
 * entire inventory — storage slots, armor and off-hand — to full durability in one go.
 *
 * <p>Behaviour:</p>
 * <ul>
 *   <li>{@code /repairall} — repairs the sender's own inventory
 *       (requires {@code sessentials.repairall}).</li>
 *   <li>{@code /repairall <player>} — repairs an online player's inventory
 *       (requires {@code sessentials.repairall.others}); the sender is told how many
 *       items were fixed.</li>
 * </ul>
 *
 * <p>Only items with a durability bar that are actually damaged are touched; the
 * repaired count reported to the sender reflects exactly those. Inventory mutation is
 * entity state, so the work always runs on the target player's Folia region thread
 * via {@link Schedulers#entity}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class RepairAllModule implements EssModule {

    @Override
    public String name() {
        return "repairall";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("repairall")
                .requires(s -> s.getSender().hasPermission("sessentials.repairall"))
                .executes(Cmds.playerExec(player -> repair(plugin, player, player)))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(Cmds.PLAYERS)
                        .requires(s -> s.getSender().hasPermission("sessentials.repairall.others"))
                        .executes(ctx -> repairOther(plugin, ctx)))
                .build(), "Repair every item in a player's inventory and armor"));
    }

    /**
     * Resolves the {@code <player>} argument and repairs that target's inventory.
     *
     * @param plugin the owning plugin
     * @param ctx    the command context holding the target name
     * @return 1 if a target was found and scheduled, 0 if the target was offline
     */
    private static int repairOther(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String targetName = StringArgumentType.getString(ctx, "player");

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            Msg.err(sender, targetName + " is not online.");
            return 0;
        }
        repair(plugin, target, sender);
        return 1;
    }

    /**
     * Repairs every damaged item in {@code target}'s inventory from the target's
     * region thread, then reports the count to the target and (when acting on someone
     * else) to the {@code sender}.
     *
     * @param plugin the owning plugin
     * @param target the player whose items are repaired
     * @param sender the command sender to confirm to when it differs from the target
     */
    private static void repair(SEssentialsPlugin plugin, Player target, CommandSender sender) {
        boolean self = sender.equals(target);
        Schedulers.entity(plugin, target, () -> {
            int repaired = repairInventory(target.getInventory());
            if (repaired == 0) {
                Msg.info(target, "Nothing needed repairing.");
            } else {
                Msg.ok(target, "Repaired " + repaired + (repaired == 1 ? " item." : " items."));
            }
            if (!self) {
                Msg.ok(sender, "Repaired " + repaired + " of " + target.getName() + "'s items.");
            }
        });
    }

    /**
     * Repairs every damaged, durability-bearing item across all 41 player inventory
     * slots (storage, armor, off-hand), writing each fixed stack back to its slot.
     *
     * @param inventory the inventory to repair
     * @return how many item stacks were restored to full durability
     */
    private static int repairInventory(PlayerInventory inventory) {
        ItemStack[] contents = inventory.getContents();
        int repaired = 0;
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (repairItem(item)) {
                inventory.setItem(slot, item);
                repaired++;
            }
        }
        return repaired;
    }

    /**
     * Zeroes the damage on a single stack if it has a durability bar and is damaged.
     *
     * @param item the stack to repair (may be {@code null} or empty)
     * @return {@code true} if the stack was changed
     */
    private static boolean repairItem(ItemStack item) {
        if (item == null || item.isEmpty() || item.getType().getMaxDurability() <= 0) {
            return false;
        }
        if (!(item.getItemMeta() instanceof Damageable damageable) || damageable.getDamage() <= 0) {
            return false;
        }
        damageable.setDamage(0);
        item.setItemMeta(damageable);
        return true;
    }
}
