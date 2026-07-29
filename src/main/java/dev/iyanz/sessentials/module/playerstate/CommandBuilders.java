package dev.iyanz.sessentials.module.playerstate;

import java.util.function.BiConsumer;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Builds the "act on yourself, or on an optional {@code [player]} target guarded by a
 * {@code .others} permission" command shape shared by several player-state commands.
 * When a target is given, the action hops to that player's region thread
 * ({@link Schedulers#entity}) before running, keeping every caller Folia-safe without
 * repeating the boilerplate.
 */
@SuppressWarnings("UnstableApiUsage")
final class CommandBuilders {

    private CommandBuilders() {
    }

    /**
     * @param literal    the command's literal name
     * @param selfPerm   permission required to run the command on the sender
     * @param othersPerm permission required to run the command on another online player
     * @param plugin     owning plugin, used to hop to the target's region thread
     * @param action     the effect to apply; receives the original command sender (for
     *                   feedback) and the affected player (the sender, or the target)
     * @return the built, unregistered command node (register it to add aliases)
     */
    static LiteralCommandNode<CommandSourceStack> selfOrTarget(String literal, String selfPerm, String othersPerm,
            SEssentialsPlugin plugin, BiConsumer<CommandSender, Player> action) {
        return Commands.literal(literal)
                .requires(s -> s.getSender().hasPermission(selfPerm))
                .executes(ctx -> {
                    Player self = Cmds.player(ctx);
                    if (self == null) {
                        return 0;
                    }
                    action.accept(ctx.getSource().getSender(), self);
                    return 1;
                })
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests(Cmds.PLAYERS)
                        .requires(s -> s.getSender().hasPermission(othersPerm))
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            String name = StringArgumentType.getString(ctx, "target");
                            Player target = Bukkit.getPlayerExact(name);
                            if (target == null) {
                                Msg.err(sender, name + " is not online.");
                                return 0;
                            }
                            Schedulers.entity(plugin, target, () -> action.accept(sender, target));
                            return 1;
                        }))
                .build();
    }
}
