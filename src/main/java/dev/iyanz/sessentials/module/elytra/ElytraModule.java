package dev.iyanz.sessentials.module.elytra;

import java.util.List;
import java.util.Map;

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
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Elytra-flight convenience commands: {@code /elytra [player]} hands out an elytra,
 * and {@code /fireworkboost} (alias {@code /boost}) hands out a stack of firework
 * rockets to use as elytra propellant.
 *
 * <p>Both commands are plain inventory gifts: no armour is equipped and no flight
 * state is toggled. Giving an item to another player is hopped onto that player's
 * Folia region thread via {@link Schedulers#entity}; giving to the sender runs
 * inline, since a normal command executor already runs on the sender's own region
 * thread.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class ElytraModule implements EssModule {

    /** Number of firework rockets granted by {@code /fireworkboost}. */
    private static final int BOOST_STACK_SIZE = 64;

    @Override
    public String name() {
        return "elytra";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            reg.register(Commands.literal("elytra")
                    .requires(s -> s.getSender().hasPermission("sessentials.elytra"))
                    .executes(Cmds.playerExec(ElytraModule::giveElytraSelf))
                    .then(Commands.argument("player", StringArgumentType.word())
                            .suggests(Cmds.PLAYERS)
                            .requires(s -> s.getSender().hasPermission("sessentials.elytra.others"))
                            .executes(ctx -> giveElytraOther(plugin, ctx)))
                    .build(), "Give yourself or a player an elytra");

            reg.register(Commands.literal("fireworkboost")
                    .requires(s -> s.getSender().hasPermission("sessentials.elytra"))
                    .executes(Cmds.playerExec(ElytraModule::giveBoost))
                    .build(), "Get a stack of firework rockets for elytra boosting", List.of("boost"));
        });
    }

    /** Gives the sender an elytra directly (already on their own region thread). */
    private static void giveElytraSelf(Player player) {
        giveItem(player, Material.ELYTRA, 1);
        Msg.ok(player, "You received an elytra.");
    }

    /** Gives {@code target} an elytra, hopped onto their Folia region thread. */
    private static int giveElytraOther(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Msg.err(sender, name + " is not online.");
            return 0;
        }

        boolean toSelf = target.equals(sender);
        if (!toSelf) {
            Msg.ok(sender, "Gave " + target.getName() + " an elytra.");
        }
        Schedulers.entity(plugin, target, () -> {
            giveItem(target, Material.ELYTRA, 1);
            Msg.ok(target, "You received an elytra.");
        });
        return 1;
    }

    /** Gives the sender a full stack of firework rockets for elytra boosting. */
    private static void giveBoost(Player player) {
        giveItem(player, Material.FIREWORK_ROCKET, BOOST_STACK_SIZE);
        Msg.ok(player, "You received a stack of firework rockets.");
    }

    /** Adds {@code amount} of {@code material} to {@code target}'s inventory, dropping any overflow. */
    private static void giveItem(Player target, Material material, int amount) {
        Map<Integer, ItemStack> overflow = target.getInventory().addItem(new ItemStack(material, amount));
        for (ItemStack leftover : overflow.values()) {
            target.getWorld().dropItemNaturally(target.getLocation(), leftover);
        }
    }
}
