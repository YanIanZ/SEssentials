package dev.iyanz.sessentials.module.fun;

import java.util.List;
import java.util.Map;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * {@code /skull <player>} (alias {@code /head}): gives the sender a
 * {@code PLAYER_HEAD} textured with the named player's skin.
 */
@SuppressWarnings("UnstableApiUsage")
final class SkullCommands {

    private SkullCommands() {
    }

    /**
     * Registers {@code /skull} (alias {@code head}) on {@code plugin}'s command
     * lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(
                Commands.literal("skull")
                        .requires(s -> s.getSender().hasPermission("sessentials.skull"))
                        .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                                .executes(SkullCommands::give))
                        .build(),
                "Get a player's head",
                List.of("head")));
    }

    /**
     * Looks up the target's offline profile and textures a new player head with it.
     * The lookup is inherently async/offline-safe on Paper, so no region-thread hop
     * is needed beyond the sender's own inventory mutation.
     */
    @SuppressWarnings("deprecation")
    private static int give(CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }

        String name = StringArgumentType.getString(ctx, "player");
        OfflinePlayer owner = Bukkit.getOfflinePlayer(name);

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(owner);
        head.setItemMeta(meta);

        Map<Integer, ItemStack> overflow = player.getInventory().addItem(head);
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        Msg.ok(player, "You received " + name + "'s head.");
        return 1;
    }
}
