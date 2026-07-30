package dev.iyanz.sessentials.module.serverextras;

import java.util.Locale;
import java.util.Map;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Registers {@code /giveall <item> [amount]}: hands the given item to every online
 * player. The material is resolved through {@link Material#matchMaterial(String)} and
 * must be an obtainable item; the amount defaults to {@code 1} and is bounded to a
 * full inventory's worth (36 stacks of 64).
 *
 * <p>Folia-safe: each recipient's inventory is mutated on that player's own region
 * thread ({@link Schedulers#entity}), and anything that does not fit is dropped at
 * the player's feet.</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class GiveAllCommand {

    /** Permission required to hand out items to everyone. */
    private static final String PERMISSION = "sessentials.giveall";

    /** Highest amount a single {@code /giveall} may hand each player (36 stacks of 64). */
    private static final int MAX_AMOUNT = 64 * 36;

    private GiveAllCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("giveall")
                .requires(s -> s.getSender().hasPermission(PERMISSION))
                .then(Commands.argument("item", StringArgumentType.word())
                        .suggests(ExtrasSuggestions.MATERIALS)
                        .executes(ctx -> giveAll(plugin, ctx, 1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, MAX_AMOUNT))
                                .executes(ctx -> giveAll(plugin, ctx,
                                        IntegerArgumentType.getInteger(ctx, "amount")))))
                .build(), "Give an item to every online player"));
    }

    /**
     * Resolves the material and schedules the give on every online player's region
     * thread, dropping overflow at each player's location.
     *
     * @param plugin    the owning plugin (for scheduler access)
     * @param ctx       the command context (carries the {@code item} argument)
     * @param rawAmount the requested per-player amount (already bounded by the parser)
     * @return 1 if the item was handed out, 0 on a bad material or empty server
     */
    private static int giveAll(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx, int rawAmount) {
        CommandSender sender = ctx.getSource().getSender();
        String itemName = StringArgumentType.getString(ctx, "item");
        Material material = Material.matchMaterial(itemName);
        if (material == null || !material.isItem()) {
            Msg.err(sender, itemName + " is not a valid item.");
            return 0;
        }

        int amount = Math.clamp(rawAmount, 1, MAX_AMOUNT);
        int recipients = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            Schedulers.entity(plugin, player, () -> {
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(new ItemStack(material, amount));
                for (ItemStack leftover : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
                Msg.ok(player, "You received " + amount + "x " + prettyName(material) + ".");
            });
            recipients++;
        }
        if (recipients == 0) {
            Msg.err(sender, "No players are online.");
            return 0;
        }
        Msg.ok(sender, "Gave " + amount + "x " + prettyName(material) + " to " + recipients + " players.");
        return 1;
    }

    /** @return the material key in readable form, e.g. {@code DIAMOND_SWORD} → {@code diamond sword}. */
    private static String prettyName(Material material) {
        return material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
