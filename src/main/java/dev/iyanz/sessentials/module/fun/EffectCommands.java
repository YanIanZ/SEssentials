package dev.iyanz.sessentials.module.fun;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * {@code /effect <player> <type|clear> [duration] [amplifier]}: applies or clears a
 * {@link PotionEffect} on a player. Duration defaults to {@value #DEFAULT_DURATION_SECONDS}
 * seconds and amplifier to {@value #DEFAULT_AMPLIFIER}; the mutation always runs on the
 * target's Folia region thread (see {@link TargetOps}).
 */
@SuppressWarnings("UnstableApiUsage")
final class EffectCommands {

    private static final int DEFAULT_DURATION_SECONDS = 30;
    private static final int DEFAULT_AMPLIFIER = 0;
    private static final int MAX_DURATION_SECONDS = 24 * 60 * 60;
    private static final int MAX_AMPLIFIER = 255;
    private static final int TICKS_PER_SECOND = 20;

    private EffectCommands() {
    }

    /**
     * Registers {@code /effect} on {@code plugin}'s command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(
                Commands.literal("effect")
                        .requires(s -> s.getSender().hasPermission("sessentials.effect"))
                        .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests(FunSuggestions.EFFECT_TYPES)
                                        .executes(ctx -> apply(plugin, ctx, DEFAULT_DURATION_SECONDS, DEFAULT_AMPLIFIER))
                                        .then(Commands.argument("duration", IntegerArgumentType.integer(1, MAX_DURATION_SECONDS))
                                                .executes(ctx -> apply(plugin, ctx,
                                                        IntegerArgumentType.getInteger(ctx, "duration"), DEFAULT_AMPLIFIER))
                                                .then(Commands.argument("amplifier", IntegerArgumentType.integer(0, MAX_AMPLIFIER))
                                                        .executes(ctx -> apply(plugin, ctx,
                                                                IntegerArgumentType.getInteger(ctx, "duration"),
                                                                IntegerArgumentType.getInteger(ctx, "amplifier")))))))
                        .build(),
                "Apply or clear a potion effect on a player"));
    }

    private static int apply(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx, int durationSeconds, int amplifier) {
        CommandSender sender = ctx.getSource().getSender();
        String targetName = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            Msg.err(sender, targetName + " is not online.");
            return 0;
        }
        if (!isSelf(sender, target) && !sender.hasPermission("sessentials.effect.others")) {
            Msg.err(sender, "You don't have permission to affect other players.");
            return 0;
        }

        String typeArg = StringArgumentType.getString(ctx, "type");
        if (typeArg.equalsIgnoreCase(FunSuggestions.CLEAR_KEYWORD)) {
            clear(plugin, sender, target);
            return 1;
        }

        PotionEffectType type = FunSuggestions.resolveEffectType(typeArg);
        if (type == null) {
            Msg.err(sender, "Unknown potion effect type.");
            return 0;
        }

        PotionEffect effect = new PotionEffect(type, durationSeconds * TICKS_PER_SECOND, amplifier);
        String label = Words.titleCase(type.getKey().getKey());
        TargetOps.run(plugin, sender, target, () -> {
            target.addPotionEffect(effect);
            Msg.ok(target, "You were given " + label + " for " + durationSeconds + "s.");
        });
        if (!isSelf(sender, target)) {
            Msg.ok(sender, "Applied " + label + " to " + target.getName() + ".");
        }
        return 1;
    }

    private static void clear(SEssentialsPlugin plugin, CommandSender sender, Player target) {
        TargetOps.run(plugin, sender, target, () -> {
            List<PotionEffectType> active = new ArrayList<>();
            for (PotionEffect effect : target.getActivePotionEffects()) {
                active.add(effect.getType());
            }
            for (PotionEffectType type : active) {
                target.removePotionEffect(type);
            }
            Msg.ok(target, "Your potion effects were cleared.");
        });
        if (!isSelf(sender, target)) {
            Msg.ok(sender, "Cleared " + target.getName() + "'s potion effects.");
        }
    }

    private static boolean isSelf(CommandSender sender, Player target) {
        return sender instanceof Player self && self.equals(target);
    }
}
