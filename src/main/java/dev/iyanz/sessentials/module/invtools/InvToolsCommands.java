package dev.iyanz.sessentials.module.invtools;

import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Registers {@code /openshulker}, {@code /sortinv} and {@code /stack}: small,
 * self-service inventory-maintenance commands. All three act only on the sender and
 * therefore run entirely on the sender's own region thread — no scheduler hop needed.
 */
@SuppressWarnings("UnstableApiUsage")
final class InvToolsCommands {

    /** First main-inventory slot (index 9), just past the 9-slot hotbar. */
    private static final int MAIN_START = 9;
    /** Last main-inventory slot (index 35), inclusive. */
    private static final int MAIN_END = 35;

    private InvToolsCommands() {
    }

    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            reg.register(Commands.literal("openshulker")
                    .requires(s -> s.getSender().hasPermission("sessentials.openshulker"))
                    .executes(Cmds.playerExec(ShulkerEditMenu::open))
                    .build(), "Open the shulker box you're holding", List.of("shulker"));

            reg.register(Commands.literal("sortinv")
                    .requires(s -> s.getSender().hasPermission("sessentials.sortinv"))
                    .executes(Cmds.playerExec(InvToolsCommands::sortInventory))
                    .build(), "Sort your main inventory", List.of("sort"));

            reg.register(Commands.literal("stack")
                    .requires(s -> s.getSender().hasPermission("sessentials.stack"))
                    .executes(Cmds.playerExec(InvToolsCommands::stackInventory))
                    .build(), "Merge same-item stacks into full ones");
        });
    }

    /** Sorts the sender's main inventory (slots 9-35) by material name and merges stacks. Leaves the hotbar untouched. */
    private static void sortInventory(Player player) {
        Inventory inventory = player.getInventory();
        int size = MAIN_END - MAIN_START + 1;
        ItemStack[] main = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            main[i] = inventory.getItem(MAIN_START + i);
        }
        ItemStack[] sorted = InventoryMerge.mergeSorted(main);
        for (int i = 0; i < size; i++) {
            inventory.setItem(MAIN_START + i, sorted[i]);
        }
        Msg.ok(player, "Inventory sorted.");
    }

    /** Merges same-item stacks across the sender's whole inventory into full stacks. */
    private static void stackInventory(Player player) {
        Inventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getStorageContents();
        inventory.setStorageContents(InventoryMerge.merge(contents));
        Msg.ok(player, "Inventory stacked.");
    }
}
