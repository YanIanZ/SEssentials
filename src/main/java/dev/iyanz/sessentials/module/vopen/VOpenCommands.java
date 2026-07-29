package dev.iyanz.sessentials.module.vopen;

import java.util.List;
import java.util.function.Consumer;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.iyanz.sessentials.command.Cmds;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.view.builder.LocationInventoryViewBuilder;

/**
 * Registers the {@link VOpenModule} commands. Each command opens a virtual, block-less
 * workstation screen via Paper's {@link MenuType} builder API, with reachability checks
 * disabled so no backing block is required.
 */
@SuppressWarnings("UnstableApiUsage")
final class VOpenCommands {

    private VOpenCommands() {
    }

    static void register(Commands reg) {
        reg.register(
                literal("enchanttable", MenuType.ENCHANTMENT),
                "Open a virtual enchanting table",
                List.of("enchanting"));

        reg.register(
                literal("brewingstand", MenuType.BREWING_STAND),
                "Open a virtual brewing stand",
                List.of("brewing"));

        reg.register(literal("furnace", MenuType.FURNACE), "Open a virtual furnace");

        reg.register(literal("blastfurnace", MenuType.BLAST_FURNACE), "Open a virtual blast furnace");

        reg.register(literal("smoker", MenuType.SMOKER), "Open a virtual smoker");

        reg.register(literal("hopper", MenuType.HOPPER), "Open a virtual hopper");
    }

    /**
     * Builds a player-only literal command node that opens {@code menu} for the sender,
     * requiring {@code sessentials.<name>}.
     *
     * @param name the command literal (and permission suffix)
     * @param menu the menu type to open
     * @param <V>  the concrete {@link InventoryView} produced by {@code menu}
     * @return the built command node
     */
    private static <V extends InventoryView> LiteralCommandNode<CommandSourceStack> literal(
            String name, MenuType.Typed<V, ? extends LocationInventoryViewBuilder<V>> menu) {
        return Commands.literal(name)
                .requires(s -> s.getSender().hasPermission("sessentials." + name))
                .executes(Cmds.playerExec(opener(menu)))
                .build();
    }

    /**
     * @param menu the menu type to open
     * @param <V>  the concrete {@link InventoryView} produced by {@code menu}
     * @return an action that opens a fresh, reachability-free virtual {@code menu} for a
     *         player on their own region thread
     */
    private static <V extends InventoryView> Consumer<Player> opener(
            MenuType.Typed<V, ? extends LocationInventoryViewBuilder<V>> menu) {
        return player -> menu.builder().checkReachable(false).build(player).open();
    }
}
