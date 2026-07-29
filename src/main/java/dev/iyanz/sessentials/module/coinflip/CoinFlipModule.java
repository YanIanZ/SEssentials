package dev.iyanz.sessentials.module.coinflip;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.economy.EconomyHook;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

/**
 * A house-versus-player coin flip: {@code /coinflip <amount>} (alias {@code /cf}) wagers
 * an amount at fair 50/50 odds. The bet is withdrawn up front; a win pays back double the
 * stake (net {@code +bet}), a loss keeps the withdrawal (net {@code -bet}). Amounts accept
 * shorthand ({@code 1k}, {@code 1.5m}, {@code 2b}). The {@code coinflip.max-bet} config
 * value (default {@value #DEFAULT_MAX_BET}) caps a single wager.
 *
 * <p>The coin is decided by {@link ThreadLocalRandom#nextBoolean()} — heads pays, tails
 * loses. Economy operations run inside the command executor, i.e. on the sender's own
 * region thread, which is Folia-safe for these self-directed balance changes.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class CoinFlipModule implements EssModule {

    /** Fallback cap for a single wager when {@code coinflip.max-bet} is unset. */
    private static final long DEFAULT_MAX_BET = 1_000_000L;

    private EconomyHook eco;
    private double maxBet;

    @Override
    public String name() {
        return "coinflip";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.eco = plugin.economy();
        this.maxBet = plugin.getConfig().getDouble("coinflip.max-bet", DEFAULT_MAX_BET);
        plugin.commands(reg -> reg.register(Commands.literal("coinflip")
                .requires(s -> s.getSender().hasPermission("sessentials.coinflip"))
                .then(Commands.argument("amount", StringArgumentType.word())
                        .executes(this::flip))
                .build(), "Bet against the house on a coin flip", List.of("cf")));
    }

    /** Places and resolves a single wager for the sending player. */
    private int flip(CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        if (!eco.available()) {
            Msg.err(player, "No economy provider found.");
            return 0;
        }
        double bet = Amounts.parse(StringArgumentType.getString(ctx, "amount"));
        if (Double.isNaN(bet) || bet <= 0) {
            Msg.err(player, "Enter a positive bet amount.");
            return 0;
        }
        if (bet > maxBet) {
            Msg.value(player, "The maximum bet is:", eco.format(maxBet));
            return 0;
        }
        UUID id = player.getUniqueId();
        if (!eco.has(id, bet)) {
            Msg.err(player, "You don't have that much to bet.");
            return 0;
        }
        if (!eco.withdraw(id, bet)) {
            Msg.err(player, "Could not place your bet.");
            return 0;
        }

        boolean won = ThreadLocalRandom.current().nextBoolean();
        if (won) {
            // Return the stake plus an equal winning (net +bet).
            eco.deposit(id, bet * 2.0);
        }
        // On a loss the up-front withdrawal is simply kept (net -bet).

        String face = won ? "Heads" : "Tails";
        String outcome = won ? " — you won" : " — you lost";
        Msg.value(player, face + outcome, eco.format(bet));
        Msg.value(player, "New balance:", eco.format(eco.balance(id)));
        return 1;
    }
}
