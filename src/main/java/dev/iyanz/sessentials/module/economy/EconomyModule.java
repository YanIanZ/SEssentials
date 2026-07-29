package dev.iyanz.sessentials.module.economy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.economy.EconomyHook;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Vault-backed economy commands: {@code /balance}, {@code /pay}, {@code /baltop} and
 * the admin {@code /eco}. Amounts accept shorthand ({@code 1k}, {@code 1.5m}, {@code 2b}).
 */
@SuppressWarnings({"UnstableApiUsage", "deprecation"})
public final class EconomyModule implements EssModule {

    private EconomyHook eco;

    @Override
    public String name() {
        return "economy";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.eco = plugin.economy();
        plugin.commands(reg -> {
            reg.register(Commands.literal("balance")
                    .requires(s -> s.getSender().hasPermission("sessentials.balance"))
                    .executes(Cmds.playerExec(p -> showBalance(p, p)))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .requires(s -> s.getSender().hasPermission("sessentials.balance.others"))
                            .executes(ctx -> {
                                OfflinePlayer t = Bukkit.getOfflinePlayer(StringArgumentType.getString(ctx, "player"));
                                showBalance(ctx.getSource().getSender(), t);
                                return 1;
                            }))
                    .build(), "Show a balance", List.of("bal", "money"));

            reg.register(Commands.literal("pay")
                    .requires(s -> s.getSender().hasPermission("sessentials.pay"))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .then(Commands.argument("amount", StringArgumentType.word())
                                    .executes(this::pay)))
                    .build(), "Pay another player");

            reg.register(Commands.literal("baltop")
                    .requires(s -> s.getSender().hasPermission("sessentials.baltop"))
                    .executes(this::baltop)
                    .build(), "Top online balances");

            reg.register(Commands.literal("eco")
                    .requires(s -> s.getSender().hasPermission("sessentials.eco"))
                    .then(Commands.literal("give").then(ecoTarget("give")))
                    .then(Commands.literal("take").then(ecoTarget("take")))
                    .then(Commands.literal("set").then(ecoTarget("set")))
                    .then(Commands.literal("reset").then(Commands.argument("player", StringArgumentType.word())
                            .suggests(Cmds.PLAYERS).executes(ctx -> ecoReset(ctx))))
                    .build(), "Admin economy operations");
        });
    }

    private com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> ecoTarget(String op) {
        return Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                .then(Commands.argument("amount", StringArgumentType.word())
                        .executes(ctx -> ecoOp(ctx, op)));
    }

    private boolean guard(CommandSender to) {
        if (!eco.available()) {
            Msg.err(to, "No economy provider found.");
            return false;
        }
        return true;
    }

    private void showBalance(CommandSender to, OfflinePlayer target) {
        if (!guard(to)) {
            return;
        }
        String who = (to instanceof Player p && p.getUniqueId().equals(target.getUniqueId()))
                ? "Your balance:" : (target.getName() + "'s balance:");
        Msg.value(to, who, eco.format(eco.balance(target.getUniqueId())));
    }

    private int pay(CommandContext<CommandSourceStack> ctx) {
        Player sender = Cmds.player(ctx);
        if (sender == null) {
            return 0;
        }
        if (!guard(sender)) {
            return 0;
        }
        Player target = Bukkit.getPlayerExact(StringArgumentType.getString(ctx, "player"));
        if (target == null) {
            Msg.err(sender, "That player is not online.");
            return 0;
        }
        if (target.getUniqueId().equals(sender.getUniqueId())) {
            Msg.err(sender, "You can't pay yourself.");
            return 0;
        }
        double amount = Amounts.parse(StringArgumentType.getString(ctx, "amount"));
        if (Double.isNaN(amount) || amount <= 0) {
            Msg.err(sender, "Invalid amount.");
            return 0;
        }
        if (!eco.has(sender.getUniqueId(), amount)) {
            Msg.err(sender, "You don't have that much.");
            return 0;
        }
        if (!eco.withdraw(sender.getUniqueId(), amount)) {
            Msg.err(sender, "Payment failed.");
            return 0;
        }
        if (!eco.deposit(target.getUniqueId(), amount)) {
            // Withdrawal succeeded but the recipient could not be credited (e.g. they are
            // at the provider's max-money cap). Refund the sender so no money is destroyed.
            eco.deposit(sender.getUniqueId(), amount);
            Msg.err(sender, "Payment failed.");
            return 0;
        }
        Msg.value(sender, "Paid " + target.getName() + ":", eco.format(amount));
        Msg.value(target, "Received from " + sender.getName() + ":", eco.format(amount));
        return 1;
    }

    private int baltop(CommandContext<CommandSourceStack> ctx) {
        CommandSender to = ctx.getSource().getSender();
        if (!guard(to)) {
            return 0;
        }
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.sort(Comparator.comparingDouble((Player p) -> eco.balance(p.getUniqueId())).reversed());
        Msg.info(to, "Top balances (online):");
        int rank = 1;
        for (Player p : players) {
            if (rank > 10) {
                break;
            }
            Msg.value(to, rank + ". " + p.getName(), eco.format(eco.balance(p.getUniqueId())));
            rank++;
        }
        return 1;
    }

    private int ecoOp(CommandContext<CommandSourceStack> ctx, String op) {
        CommandSender to = ctx.getSource().getSender();
        if (!guard(to)) {
            return 0;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(StringArgumentType.getString(ctx, "player"));
        double amount = Amounts.parse(StringArgumentType.getString(ctx, "amount"));
        if (Double.isNaN(amount) || amount < 0) {
            Msg.err(to, "Invalid amount.");
            return 0;
        }
        UUID id = target.getUniqueId();
        switch (op) {
            case "give" -> eco.deposit(id, amount);
            case "take" -> eco.withdraw(id, amount);
            case "set" -> {
                double current = eco.balance(id);
                if (current > 0) {
                    eco.withdraw(id, current);
                }
                eco.deposit(id, amount);
            }
            default -> {
                return 0;
            }
        }
        Msg.value(to, target.getName() + " balance:", eco.format(eco.balance(id)));
        return 1;
    }

    private int ecoReset(CommandContext<CommandSourceStack> ctx) {
        CommandSender to = ctx.getSource().getSender();
        if (!guard(to)) {
            return 0;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(StringArgumentType.getString(ctx, "player"));
        double current = eco.balance(target.getUniqueId());
        if (current > 0) {
            eco.withdraw(target.getUniqueId(), current);
        }
        Msg.value(to, target.getName() + " balance:", eco.format(eco.balance(target.getUniqueId())));
        return 1;
    }
}
