package dev.iyanz.sessentials.module.smite;

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
 * Smite module: {@code /smite <player>} strikes a real, damaging lightning bolt at an
 * online player's location.
 *
 * <p>Behaviour:</p>
 * <ul>
 *   <li>{@code /smite <player>} — spawns lightning at the target's position, with the
 *       usual visual, sound and damage effects
 *       (requires {@code sessentials.smite}).</li>
 * </ul>
 *
 * <p>Spawning the bolt reads the target's location and mutates the target's world, so
 * it runs on the target's Folia region thread via {@link Schedulers#entity} using
 * {@link org.bukkit.World#strikeLightning(org.bukkit.Location)}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class SmiteModule implements EssModule {

    @Override
    public String name() {
        return "smite";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("smite")
                .requires(s -> s.getSender().hasPermission("sessentials.smite"))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(Cmds.PLAYERS)
                        .executes(ctx -> smite(plugin, ctx)))
                .build(), "Strike lightning at a player"));
    }

    /**
     * Resolves the {@code <player>} argument and strikes lightning at that target from
     * the target's region thread.
     *
     * @param plugin the owning plugin
     * @param ctx    the command context holding the target name
     * @return 1 if a target was found and scheduled, 0 if the target was offline
     */
    private static int smite(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String targetName = StringArgumentType.getString(ctx, "player");

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            Msg.err(sender, targetName + " is not online.");
            return 0;
        }
        Schedulers.entity(plugin, target,
                () -> target.getWorld().strikeLightning(target.getLocation()));
        Msg.ok(sender, "Smote " + target.getName() + ".");
        return 1;
    }
}
