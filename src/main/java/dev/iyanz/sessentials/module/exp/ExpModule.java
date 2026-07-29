package dev.iyanz.sessentials.module.exp;

import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
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

/**
 * Experience-point commands: {@code /exp} (alias {@code /xp}) reports the sender's
 * level and total experience, and the {@code give}/{@code set}/{@code take}
 * subcommands let admins grant, set, or remove an online player's experience.
 *
 * <p>The bare {@code /exp} command carries no permission requirement of its own
 * (matching this codebase's convention for purely informational bare commands, see
 * e.g. {@code /kit}/{@code /kits}), so that a subcommand's own {@code .requires()}
 * is never masked by a parent-node permission check — Brigadier gates an entire
 * subtree on an ancestor's {@code requires()}, so gating the shared "exp" literal
 * would force every admin subcommand to also need that permission.</p>
 *
 * <p>"Total experience" is the point count implied by level + progress (see
 * {@link ExpMath}) — the value {@code give}/{@code set}/{@code take} operate on —
 * rather than Bukkit's separate {@code getTotalExperience()} lifetime counter, which
 * can drift from level/progress after direct {@code setLevel}/{@code setExp} calls.</p>
 *
 * <p>Every target subcommand hops to the target player's region thread via
 * {@link Schedulers#entity} before touching their experience state (Folia-safe).</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class ExpModule implements EssModule {

    @Override
    public String name() {
        return "exp";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("exp")
                .executes(Cmds.playerExec(ExpModule::showSelf))
                .then(Commands.literal("give")
                        .requires(s -> s.getSender().hasPermission("sessentials.exp.give"))
                        .then(targetArg(plugin, 1, (sender, player, amount) -> {
                            player.giveExp(amount);
                            Msg.ok(player, "You received " + amount + " experience.");
                            if (sender != player) {
                                Msg.ok(sender, "Gave " + player.getName() + " " + amount + " experience.");
                            }
                        })))
                .then(Commands.literal("set")
                        .requires(s -> s.getSender().hasPermission("sessentials.exp.set"))
                        .then(targetArg(plugin, 0, (sender, player, amount) -> {
                            player.setExp(0f);
                            player.setLevel(0);
                            player.giveExp(amount);
                            Msg.ok(player, "Your experience was set to " + amount + ".");
                            if (sender != player) {
                                Msg.ok(sender, "Set " + player.getName() + "'s experience to " + amount + ".");
                            }
                        })))
                .then(Commands.literal("take")
                        .requires(s -> s.getSender().hasPermission("sessentials.exp.take"))
                        .then(targetArg(plugin, 1, (sender, player, amount) -> {
                            player.giveExp(-amount);
                            Msg.info(player, amount + " experience was taken from you.");
                            if (sender != player) {
                                Msg.ok(sender, "Took " + amount + " experience from " + player.getName() + ".");
                            }
                        })))
                .build(), "Show, give, set, or take experience", List.of("xp")));
    }

    /** Reports the sender's own level and equivalent total experience points. */
    private static void showSelf(Player player) {
        long total = ExpMath.totalExperience(player.getLevel(), player.getExp());
        Msg.value(player, "Level:", String.valueOf(player.getLevel()));
        Msg.value(player, "Total exp:", String.valueOf(total));
    }

    /**
     * Builds the shared {@code <player> <amount>} argument shape for the
     * give/set/take subcommands: looks up the online target, hops to its region
     * thread, and runs {@code action} there (Folia-safe).
     *
     * @param plugin    owning plugin, used to hop to the target's region thread
     * @param minAmount the smallest accepted amount ({@code 0} for {@code set},
     *                  {@code 1} for {@code give}/{@code take})
     * @param action    receives the command sender, the resolved target, and the
     *                  parsed amount, and applies the effect (messaging both)
     * @return the built {@code <player>} argument node (not yet registered)
     */
    private static RequiredArgumentBuilder<CommandSourceStack, String> targetArg(
            SEssentialsPlugin plugin, int minAmount, ExpAction action) {
        return Commands.argument("player", StringArgumentType.word())
                .suggests(Cmds.PLAYERS)
                .then(Commands.argument("amount", IntegerArgumentType.integer(minAmount))
                        .executes(ctx -> apply(plugin, ctx, action)));
    }

    private static int apply(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx, ExpAction action) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Msg.err(sender, name + " is not online.");
            return 0;
        }
        Schedulers.entity(plugin, target, () -> action.apply(sender, target, amount));
        return 1;
    }

    /** The effect applied by an {@code exp} target subcommand (give/set/take). */
    @FunctionalInterface
    private interface ExpAction {
        void apply(CommandSender sender, Player player, int amount);
    }
}
