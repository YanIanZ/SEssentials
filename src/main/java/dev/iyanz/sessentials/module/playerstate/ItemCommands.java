package dev.iyanz.sessentials.module.playerstate;

import java.util.List;

import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Item-utility commands: {@code /hat} (wear the held item as a helmet) and
 * {@code /repair} (alias {@code /fix}), which clears durability damage on the held
 * item, or the whole inventory with {@code /repair all} (permission
 * {@code sessentials.repair.all}).
 */
@SuppressWarnings("UnstableApiUsage")
final class ItemCommands {

    private ItemCommands() {
    }

    static void register(Commands reg) {
        reg.register(
                Commands.literal("hat")
                        .requires(s -> s.getSender().hasPermission("sessentials.hat"))
                        .executes(Cmds.playerExec(ItemCommands::wearHat))
                        .build(),
                "Wear the item in your hand as a helmet");

        reg.register(
                Commands.literal("repair")
                        .requires(s -> s.getSender().hasPermission("sessentials.repair"))
                        .executes(Cmds.playerExec(ItemCommands::repairHeld))
                        .then(Commands.literal("all")
                                .requires(s -> s.getSender().hasPermission("sessentials.repair.all"))
                                .executes(Cmds.playerExec(ItemCommands::repairAll)))
                        .build(),
                "Repair the item in your hand (or 'all' for your whole inventory)",
                List.of("fix"));
    }

    /** Swaps the held item into the helmet slot, moving the previous helmet to hand. */
    private static void wearHat(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack inHand = inventory.getItemInMainHand();
        if (inHand.isEmpty()) {
            Msg.err(player, "You are not holding anything.");
            return;
        }
        ItemStack previousHelmet = inventory.getHelmet();
        inventory.setHelmet(inHand.clone());
        inventory.setItemInMainHand(previousHelmet == null ? new ItemStack(Material.AIR) : previousHelmet);
        Msg.ok(player, "Enjoy your new hat.");
    }

    private static void repairHeld(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.isEmpty()) {
            Msg.err(player, "You are not holding anything.");
            return;
        }
        if (repair(item)) {
            Msg.ok(player, "Repaired your held item.");
        } else {
            Msg.err(player, "That item cannot be repaired.");
        }
    }

    private static void repairAll(Player player) {
        PlayerInventory inventory = player.getInventory();
        int repaired = repairArray(inventory.getContents())
                + repairArray(inventory.getArmorContents())
                + repairArray(inventory.getExtraContents());
        if (repaired > 0) {
            Msg.ok(player, "Repaired " + repaired + " item(s).");
        } else {
            Msg.info(player, "Nothing to repair.");
        }
    }

    private static int repairArray(ItemStack[] items) {
        int count = 0;
        for (ItemStack item : items) {
            if (item != null && !item.isEmpty() && repair(item)) {
                count++;
            }
        }
        return count;
    }

    /** Clears durability damage on {@code item} if its meta supports it. */
    private static boolean repair(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return false;
        }
        damageable.setDamage(0);
        item.setItemMeta(meta);
        return true;
    }
}
