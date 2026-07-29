package dev.iyanz.sessentials.module.counter;

import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;

/**
 * Simple named integer counters, persisted server-wide (see {@link Counters}).
 *
 * <p>Commands: {@code /counter <name>} reports a counter's current value (an
 * unreferenced name reads as {@code 0}), {@code /counter <name> add [amount]} adds
 * {@code amount} (default {@code 1}; may be negative to subtract) and reports the
 * new value, {@code /counter <name> set <amount>} sets it outright, {@code /counter
 * <name> reset} zeroes it, and {@code /counters} lists every known counter with its
 * current value.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class CounterModule implements EssModule {

    private static final String PERMISSION = "sessentials.counter";

    @Override
    public String name() {
        return "counter";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            reg.register(
                    Commands.literal("counter")
                            .requires(s -> s.getSender().hasPermission(PERMISSION))
                            .executes(ctx -> {
                                Msg.err(ctx.getSource().getSender(), "Usage: /counter <name> [add|set|reset].");
                                return 0;
                            })
                            .then(Commands.argument("name", StringArgumentType.word())
                                    .suggests(CounterSuggestions.of(plugin))
                                    .executes(ctx -> show(plugin, ctx))
                                    .then(Commands.literal("add")
                                            .executes(ctx -> add(plugin, ctx, 1))
                                            .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                    .executes(ctx -> add(plugin, ctx,
                                                            IntegerArgumentType.getInteger(ctx, "amount")))))
                                    .then(Commands.literal("set")
                                            .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                    .executes(ctx -> set(plugin, ctx))))
                                    .then(Commands.literal("reset")
                                            .executes(ctx -> reset(plugin, ctx))))
                            .build(),
                    "Show or modify a named counter");

            reg.register(
                    Commands.literal("counters")
                            .requires(s -> s.getSender().hasPermission(PERMISSION))
                            .executes(ctx -> list(plugin, ctx.getSource().getSender()))
                            .build(),
                    "List all counters");
        });
    }

    /** Reports the counter named by the {@code name} argument. */
    private static int show(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        report(plugin, ctx.getSource().getSender(), nameArg(ctx));
        return 1;
    }

    /** Adds {@code amount} to the counter named by the {@code name} argument, then reports it. */
    private static int add(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx, int amount) {
        String name = nameArg(ctx);
        Counters.set(plugin, name, Counters.get(plugin, name) + amount);
        report(plugin, ctx.getSource().getSender(), name);
        return 1;
    }

    /** Sets the counter named by the {@code name} argument to the {@code amount} argument, then reports it. */
    private static int set(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        String name = nameArg(ctx);
        Counters.set(plugin, name, IntegerArgumentType.getInteger(ctx, "amount"));
        report(plugin, ctx.getSource().getSender(), name);
        return 1;
    }

    /** Zeroes the counter named by the {@code name} argument, then reports it. */
    private static int reset(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        String name = nameArg(ctx);
        Counters.set(plugin, name, 0);
        report(plugin, ctx.getSource().getSender(), name);
        return 1;
    }

    /** Lists every known counter and its current value. */
    private static int list(SEssentialsPlugin plugin, CommandSender sender) {
        List<String> names = Counters.names(plugin);
        if (names.isEmpty()) {
            Msg.info(sender, "There are no counters set.");
            return 1;
        }
        StringBuilder summary = new StringBuilder();
        for (String name : names) {
            if (summary.length() > 0) {
                summary.append(", ");
            }
            summary.append(name).append('=').append(Counters.get(plugin, name));
        }
        Msg.value(sender, "Counters:", summary.toString());
        return 1;
    }

    /** @return the lower-cased {@code name} argument of {@code ctx}. */
    private static String nameArg(CommandContext<CommandSourceStack> ctx) {
        return StringArgumentType.getString(ctx, "name").toLowerCase(Locale.ROOT);
    }

    /** Reports {@code name}'s current value to {@code sender}. */
    private static void report(SEssentialsPlugin plugin, CommandSender sender, String name) {
        Msg.value(sender, "Counter \"" + name + "\":", String.valueOf(Counters.get(plugin, name)));
    }
}
