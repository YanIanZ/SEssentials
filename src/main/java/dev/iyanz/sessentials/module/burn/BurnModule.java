package dev.iyanz.sessentials.module.burn;

import com.mojang.brigadier.arguments.IntegerArgumentType;
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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Burn module: {@code /burn <player> [seconds]} sets an online player on fire for the
 * given duration (default {@value #DEFAULT_SECONDS} seconds).
 *
 * <p>Behaviour:</p>
 * <ul>
 *   <li>{@code /burn <player>} — ignites the target for the default duration.</li>
 *   <li>{@code /burn <player> <seconds>} — ignites the target for 1–{@value #MAX_SECONDS}
 *       seconds.</li>
 * </ul>
 *
 * <p>The whole command requires {@code sessentials.burn}; there is no self-only form
 * (a target is always named — the sender may of course name themselves). Setting fire
 * ticks mutates the target entity, so it runs on the target's Folia region thread via
 * {@link Schedulers#entity}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class BurnModule implements EssModule {

    /** Burn duration in seconds when none is given. */
    private static final int DEFAULT_SECONDS = 5;
    /** Upper bound on the requested burn duration, in seconds. */
    private static final int MAX_SECONDS = 3600;
    /** Game ticks per second, for converting seconds to fire ticks. */
    private static final int TICKS_PER_SECOND = 20;

    @Override
    public String name() {
        return "burn";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("burn")
                .requires(s -> s.getSender().hasPermission("sessentials.burn"))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(Cmds.PLAYERS)
                        .executes(ctx -> burn(plugin, ctx, DEFAULT_SECONDS))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, MAX_SECONDS))
                                .executes(ctx -> burn(plugin, ctx,
                                        IntegerArgumentType.getInteger(ctx, "seconds")))))
                .build(), "Set a player on fire for a number of seconds"));
    }

    /**
     * Resolves the {@code <player>} argument and ignites that target for
     * {@code seconds} seconds on the target's region thread.
     *
     * @param plugin  the owning plugin
     * @param ctx     the command context holding the target name
     * @param seconds how long the target should burn
     * @return 1 if a target was found and scheduled, 0 if the target was offline
     */
    private static int burn(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx, int seconds) {
        CommandSender sender = ctx.getSource().getSender();
        String targetName = StringArgumentType.getString(ctx, "player");

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            Msg.err(sender, targetName + " is not online.");
            return 0;
        }
        Schedulers.entity(plugin, target, () -> {
            target.setFireTicks(seconds * TICKS_PER_SECOND);
            Msg.info(target, "You caught fire!");
        });
        Msg.ok(sender, "Set " + target.getName() + " on fire for " + seconds
                + (seconds == 1 ? " second." : " seconds."));
        return 1;
    }
}
