package dev.iyanz.sessentials.module.lottery;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.economy.EconomyHook;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * A server-wide lottery. Players buy tickets with {@code /lottery buy}; each ticket is
 * one entry in an in-memory pool and its price feeds a shared pot. On a repeating
 * timer (and on the admin {@code /lottery draw}) a random ticket is drawn and its
 * holder is paid the whole pot.
 *
 * <p>Reads three keys once at {@link #enable}: {@code lottery.enabled} (default
 * {@code false} — the feature is off unless switched on), {@code lottery.ticket-price}
 * (default {@code 1000}, accepts shorthand like {@code 1.5k}) and
 * {@code lottery.draw-interval-seconds} (default {@code 3600}). When disabled the module
 * registers nothing. All money operations require a Vault economy provider and are
 * guarded per command; the pool and pot live only in memory and reset on restart.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class LotteryModule implements EssModule {

    /** Ticks per second, used to convert the configured interval to Bukkit ticks. */
    private static final long TICKS_PER_SECOND = 20L;

    /** Fallback ticket price when the configured value is missing or invalid. */
    private static final double DEFAULT_TICKET_PRICE = 1000.0;

    /** Fallback draw interval when the configured value is missing or non-positive. */
    private static final long DEFAULT_INTERVAL_SECONDS = 3600L;

    private EconomyHook eco;
    private LotteryService service;
    private double ticketPrice;
    private ScheduledTask task;

    @Override
    public String name() {
        return "lottery";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        if (!plugin.getConfig().getBoolean("lottery.enabled", false)) {
            return;
        }
        this.eco = plugin.economy();
        this.ticketPrice = resolveTicketPrice(plugin);
        long intervalSeconds = plugin.getConfig().getLong("lottery.draw-interval-seconds", DEFAULT_INTERVAL_SECONDS);
        if (intervalSeconds <= 0) {
            intervalSeconds = DEFAULT_INTERVAL_SECONDS;
        }
        this.service = new LotteryService(plugin, eco, intervalSeconds);

        plugin.commands(reg -> reg.register(Commands.literal("lottery")
                .requires(s -> s.getSender().hasPermission("sessentials.lottery"))
                .executes(this::info)
                .then(Commands.literal("buy")
                        .executes(ctx -> buy(ctx, 1))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                .executes(ctx -> buy(ctx, IntegerArgumentType.getInteger(ctx, "count")))))
                .then(Commands.literal("info")
                        .executes(this::info))
                .then(Commands.literal("draw")
                        .requires(s -> s.getSender().hasPermission("sessentials.lottery.admin"))
                        .executes(this::draw))
                .build(), "Server lottery: buy tickets and win the pot"));

        long periodTicks = intervalSeconds * TICKS_PER_SECOND;
        this.task = Schedulers.asyncRepeating(plugin, service::runDraw, periodTicks, periodTicks);
    }

    @Override
    public void disable(SEssentialsPlugin plugin) {
        if (task != null) {
            task.cancel();
        }
    }

    /** @return the ticket price from config (shorthand accepted), or the default when invalid. */
    private static double resolveTicketPrice(SEssentialsPlugin plugin) {
        double parsed = Amounts.parse(plugin.getConfig().getString("lottery.ticket-price", "1000"));
        return Double.isNaN(parsed) ? DEFAULT_TICKET_PRICE : parsed;
    }

    private boolean guard(CommandSender to) {
        if (!eco.available()) {
            Msg.err(to, "No economy provider found.");
            return false;
        }
        return true;
    }

    private int buy(CommandContext<CommandSourceStack> ctx, int count) {
        Player buyer = Cmds.player(ctx);
        if (buyer == null) {
            return 0;
        }
        if (!guard(buyer)) {
            return 0;
        }
        double total = ticketPrice * count;
        if (!eco.has(buyer.getUniqueId(), total)) {
            Msg.err(buyer, "You can't afford " + count + " ticket(s) at " + eco.format(ticketPrice) + " each.");
            return 0;
        }
        if (!eco.withdraw(buyer.getUniqueId(), total)) {
            Msg.err(buyer, "Ticket purchase failed.");
            return 0;
        }
        int held = service.recordPurchase(buyer.getUniqueId(), count, total);
        Msg.value(buyer, "Bought " + count + " ticket(s) for", eco.format(total));
        Msg.value(buyer, "Your tickets:", String.valueOf(held));
        Msg.value(buyer, "Current pot:", eco.format(service.pot()));
        return 1;
    }

    private int info(CommandContext<CommandSourceStack> ctx) {
        CommandSender to = ctx.getSource().getSender();
        if (!guard(to)) {
            return 0;
        }
        Msg.info(to, "Lottery");
        Msg.value(to, "Pot:", eco.format(service.pot()));
        Msg.value(to, "Ticket price:", eco.format(ticketPrice));
        Msg.value(to, "Total tickets:", String.valueOf(service.totalTickets()));
        if (to instanceof Player player) {
            Msg.value(to, "Your tickets:", String.valueOf(service.ticketsOf(player.getUniqueId())));
        }
        Msg.value(to, "Next draw in:", LotteryService.formatDuration(service.secondsUntilDraw()));
        Msg.info(to, "The pot is in memory only and resets on restart.");
        return 1;
    }

    private int draw(CommandContext<CommandSourceStack> ctx) {
        CommandSender to = ctx.getSource().getSender();
        if (!guard(to)) {
            return 0;
        }
        service.runDraw();
        Msg.ok(to, "Lottery draw executed.");
        return 1;
    }
}
