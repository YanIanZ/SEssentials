package dev.iyanz.sessentials.module.fun;

import java.util.Map;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.WritableBookMeta;

/**
 * {@code /book}: hands out a blank writable book, or transforms/copies a held
 * written book.
 * <ul>
 *   <li>{@code /book} — gives the sender a new {@code WRITABLE_BOOK}.</li>
 *   <li>{@code /book unsign} — turns the held {@code WRITTEN_BOOK} back into an
 *       editable {@code WRITABLE_BOOK} with the same pages, replacing it in-hand.</li>
 *   <li>{@code /book copy} — copies the held {@code WRITTEN_BOOK}'s pages into a new
 *       {@code WRITABLE_BOOK}, added to the inventory alongside the original.</li>
 * </ul>
 */
@SuppressWarnings("UnstableApiUsage")
final class BookCommands {

    private BookCommands() {
    }

    /**
     * Registers {@code /book} (with {@code unsign}/{@code copy} subcommands) on
     * {@code plugin}'s command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(
                Commands.literal("book")
                        .requires(s -> s.getSender().hasPermission("sessentials.book"))
                        .executes(Cmds.playerExec(BookCommands::giveBlank))
                        .then(Commands.literal("unsign").executes(Cmds.playerExec(BookCommands::unsign)))
                        .then(Commands.literal("copy").executes(Cmds.playerExec(BookCommands::copy)))
                        .build(),
                "Get a writable book, or unsign/copy a written one"));
    }

    private static void giveBlank(Player player) {
        giveOrDrop(player, new ItemStack(Material.WRITABLE_BOOK));
        Msg.ok(player, "You received a writable book.");
    }

    private static void unsign(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() != Material.WRITTEN_BOOK) {
            Msg.err(player, "You must hold a written book.");
            return;
        }
        player.getInventory().setItemInMainHand(toWritable(hand));
        Msg.ok(player, "The book is now writable again.");
    }

    private static void copy(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() != Material.WRITTEN_BOOK) {
            Msg.err(player, "You must hold a written book.");
            return;
        }
        giveOrDrop(player, toWritable(hand));
        Msg.ok(player, "Copied the book's pages into a new writable book.");
    }

    /** Builds a new {@code WRITABLE_BOOK} carrying {@code writtenBook}'s pages. */
    private static ItemStack toWritable(ItemStack writtenBook) {
        BookMeta written = (BookMeta) writtenBook.getItemMeta();
        ItemStack writable = new ItemStack(Material.WRITABLE_BOOK);
        WritableBookMeta meta = (WritableBookMeta) writable.getItemMeta();
        meta.setPages(written.getPages());
        writable.setItemMeta(meta);
        return writable;
    }

    private static void giveOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }
}
