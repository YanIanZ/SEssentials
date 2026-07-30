package dev.iyanz.sessentials.module.attributes;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Numeric player attribute/stat commands: {@code /hunger}, {@code /saturation},
 * {@code /air}, {@code /maxhp} and {@code /scale}.
 *
 * <p>Each command sets a single numeric stat on the sender, or on an optional
 * {@code [player]} target guarded by the {@code sessentials.<command>.others}
 * permission (the self form requires {@code sessentials.<command>}). All commands
 * share one registration shape: a required {@code amount} argument, an optional
 * trailing {@code target} name suggested from the online player list.</p>
 *
 * <p>Folia-safe: every stat mutation is dispatched to the affected player's own
 * region thread via {@link Schedulers#entity}, and both confirmation messages are
 * sent from that hop so they always reflect the value actually applied (e.g. after
 * {@code /air} clamps to the target's maximum air supply).</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class AttributesModule implements EssModule {

    private SEssentialsPlugin plugin;

    @Override
    public String name() {
        return "attributes";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.plugin = plugin;
        plugin.commands(reg -> {
            registerStat(reg, "hunger", "Set your or a player's hunger level (0-20)",
                    IntegerArgumentType.integer(0, 20), "hunger",
                    AttributesModule::applyHunger);
            registerStat(reg, "saturation", "Set your or a player's saturation (0-20)",
                    IntegerArgumentType.integer(0, 20), "saturation",
                    AttributesModule::applySaturation);
            registerStat(reg, "air", "Set your or a player's remaining air supply (ticks)",
                    IntegerArgumentType.integer(0), "air supply",
                    AttributesModule::applyAir);
            registerStat(reg, "maxhp", "Set your or a player's maximum health (1-1024)",
                    IntegerArgumentType.integer(1, 1024), "max health",
                    AttributesModule::applyMaxHealth);
            registerStat(reg, "scale", "Set your or a player's model scale (0.1-16.0)",
                    DoubleArgumentType.doubleArg(0.1, 16.0), "scale",
                    AttributesModule::applyScale);
        });
    }

    // --- registration -------------------------------------------------------------

    /**
     * Registers one {@code /<command> <amount> [player]} stat command. The self form
     * requires {@code sessentials.<command>}; the target form additionally requires
     * {@code sessentials.<command>.others} and tab-completes online player names.
     *
     * @param reg         the command registrar
     * @param command     the command literal (also the permission stem)
     * @param description the help description
     * @param amountType  the Brigadier type (with range) of the {@code amount} argument
     * @param statLabel   the human-readable stat name used in confirmation messages
     * @param applier     the mutation to run on the affected player's region thread
     */
    private void registerStat(Commands reg, String command, String description,
                              ArgumentType<? extends Number> amountType, String statLabel,
                              StatApplier applier) {
        LiteralCommandNode<CommandSourceStack> node = Commands.literal(command)
                .requires(s -> s.getSender().hasPermission("sessentials." + command))
                .then(Commands.argument("amount", amountType)
                        .executes(ctx -> {
                            Player self = Cmds.player(ctx);
                            if (self == null) {
                                return 0;
                            }
                            apply(self, self, amount(ctx), statLabel, applier);
                            return 1;
                        })
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests(Cmds.PLAYERS)
                                .requires(s -> s.getSender().hasPermission("sessentials." + command + ".others"))
                                .executes(ctx -> applyToNamed(ctx, statLabel, applier))))
                .build();
        reg.register(node, description);
    }

    /** Reads the parsed {@code amount} argument as a double, whatever its declared type. */
    private static double amount(CommandContext<CommandSourceStack> ctx) {
        return ctx.getArgument("amount", Number.class).doubleValue();
    }

    /** Resolves the named online target for a {@code .others} invocation and applies the stat. */
    private int applyToNamed(CommandContext<CommandSourceStack> ctx, String statLabel, StatApplier applier) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "target");
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Msg.err(sender, name + " is not online.");
            return 0;
        }
        apply(sender, target, amount(ctx), statLabel, applier);
        return 1;
    }

    /**
     * Runs {@code applier} on {@code target}'s region thread, then confirms to the
     * target (and to {@code actor} when acting on someone else) with the value that
     * was actually applied. A {@link Double#NaN} result means the stat is unavailable
     * on the target and only an error is sent to the actor.
     */
    private void apply(CommandSender actor, Player target, double requested, String statLabel, StatApplier applier) {
        Schedulers.entity(plugin, target, () -> {
            double applied = applier.apply(target, requested);
            if (Double.isNaN(applied)) {
                Msg.err(actor, target.getName() + " has no " + statLabel + " attribute.");
                return;
            }
            String value = format(applied);
            Msg.ok(target, "Your " + statLabel + " is now " + value + ".");
            if (!target.equals(actor)) {
                Msg.ok(actor, "Set " + target.getName() + "'s " + statLabel + " to " + value + ".");
            }
        });
    }

    /** Formats a stat value: whole numbers without a decimal point, others as-is. */
    private static String format(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    // --- stat appliers (all run on the target's region thread) --------------------

    /** Sets the food (hunger) level, 0-20. */
    private static double applyHunger(Player target, double requested) {
        target.setFoodLevel((int) requested);
        return requested;
    }

    /** Sets the food saturation, 0-20. */
    private static double applySaturation(Player target, double requested) {
        target.setSaturation((float) requested);
        return requested;
    }

    /** Sets the remaining air supply in ticks, clamped to the target's maximum air. */
    private static double applyAir(Player target, double requested) {
        int air = (int) Math.min(requested, target.getMaximumAir());
        target.setRemainingAir(air);
        return air;
    }

    /** Sets the {@link Attribute#MAX_HEALTH} base value, then clamps current health under it. */
    private static double applyMaxHealth(Player target, double requested) {
        AttributeInstance attribute = target.getAttribute(Attribute.MAX_HEALTH);
        if (attribute == null) {
            return Double.NaN;
        }
        attribute.setBaseValue(requested);
        if (target.getHealth() > attribute.getValue()) {
            target.setHealth(attribute.getValue());
        }
        return requested;
    }

    /** Sets the {@link Attribute#SCALE} base value (model size multiplier). */
    private static double applyScale(Player target, double requested) {
        AttributeInstance attribute = target.getAttribute(Attribute.SCALE);
        if (attribute == null) {
            return Double.NaN;
        }
        attribute.setBaseValue(requested);
        return requested;
    }

    /** A single stat mutation, executed on the affected player's region thread. */
    @FunctionalInterface
    private interface StatApplier {

        /**
         * Applies the requested value to the target.
         *
         * @param target    the player being modified
         * @param requested the value parsed from the command (already range-checked)
         * @return the value actually applied (possibly clamped), or {@link Double#NaN}
         *         when the underlying attribute is missing on this player
         */
        double apply(Player target, double requested);
    }
}
