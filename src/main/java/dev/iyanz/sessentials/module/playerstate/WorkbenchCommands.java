package dev.iyanz.sessentials.module.playerstate;

import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Virtual-UI openers for the sender's own workstation screens: crafting table,
 * anvil, ender chest (optionally another player's, with
 * {@code sessentials.enderchest.others}), grindstone, cartography table, loom,
 * smithing table and stonecutter. None of these place a real block; they open the
 * client-side screen directly via Paper's {@code openX(Location, force)} API.
 */
@SuppressWarnings({"UnstableApiUsage", "deprecation"})
final class WorkbenchCommands {

    private WorkbenchCommands() {
    }

    static void register(Commands reg, SEssentialsPlugin plugin) {
        reg.register(
                Commands.literal("workbench")
                        .requires(s -> s.getSender().hasPermission("sessentials.workbench"))
                        .executes(Cmds.playerExec(p -> p.openWorkbench(null, true)))
                        .build(),
                "Open a virtual crafting table",
                List.of("wb", "craft"));

        reg.register(
                Commands.literal("anvil")
                        .requires(s -> s.getSender().hasPermission("sessentials.anvil"))
                        .executes(Cmds.playerExec(p -> p.openAnvil(null, true)))
                        .build(),
                "Open a virtual anvil");

        reg.register(enderChestNode(plugin), "Open your (or a player's) ender chest", List.of("ec", "echest"));

        reg.register(
                Commands.literal("grindstone")
                        .requires(s -> s.getSender().hasPermission("sessentials.grindstone"))
                        .executes(Cmds.playerExec(p -> p.openGrindstone(null, true)))
                        .build(),
                "Open a virtual grindstone");

        reg.register(
                Commands.literal("cartography")
                        .requires(s -> s.getSender().hasPermission("sessentials.cartography"))
                        .executes(Cmds.playerExec(p -> p.openCartographyTable(null, true)))
                        .build(),
                "Open a virtual cartography table");

        reg.register(
                Commands.literal("loom")
                        .requires(s -> s.getSender().hasPermission("sessentials.loom"))
                        .executes(Cmds.playerExec(p -> p.openLoom(null, true)))
                        .build(),
                "Open a virtual loom");

        reg.register(
                Commands.literal("smithingtable")
                        .requires(s -> s.getSender().hasPermission("sessentials.smithingtable"))
                        .executes(Cmds.playerExec(p -> p.openSmithingTable(null, true)))
                        .build(),
                "Open a virtual smithing table",
                List.of("smithing"));

        reg.register(
                Commands.literal("stonecutter")
                        .requires(s -> s.getSender().hasPermission("sessentials.stonecutter"))
                        .executes(Cmds.playerExec(p -> p.openStonecutter(null, true)))
                        .build(),
                "Open a virtual stonecutter");
    }

    /**
     * Builds {@code /enderchest [player]}: opens the sender's own ender chest, or -
     * with {@code sessentials.enderchest.others} - a target's, by hopping to the
     * target's region thread to read it and back to the sender's to display it.
     */
    private static LiteralCommandNode<CommandSourceStack> enderChestNode(SEssentialsPlugin plugin) {
        return Commands.literal("enderchest")
                .requires(s -> s.getSender().hasPermission("sessentials.enderchest"))
                .executes(Cmds.playerExec(p -> p.openInventory(p.getEnderChest())))
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests(Cmds.PLAYERS)
                        .requires(s -> s.getSender().hasPermission("sessentials.enderchest.others"))
                        .executes(ctx -> {
                            Player viewer = Cmds.player(ctx);
                            if (viewer == null) {
                                return 0;
                            }
                            CommandSender sender = ctx.getSource().getSender();
                            String name = StringArgumentType.getString(ctx, "target");
                            Player target = Bukkit.getPlayerExact(name);
                            if (target == null) {
                                Msg.err(sender, name + " is not online.");
                                return 0;
                            }
                            Schedulers.entity(plugin, target, () -> {
                                Inventory enderChest = target.getEnderChest();
                                Schedulers.entity(plugin, viewer, () -> viewer.openInventory(enderChest));
                            });
                            return 1;
                        }))
                .build();
    }
}
