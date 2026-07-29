package dev.iyanz.sessentials.module.nightvision;

import java.util.List;

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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Night-vision module: {@code /nightvision} (alias {@code /nv}) toggles an infinite
 * night-vision effect on a player.
 *
 * <p>Behaviour:</p>
 * <ul>
 *   <li>{@code /nightvision} — toggles the sender's own night vision
 *       (requires {@code sessentials.nightvision}).</li>
 *   <li>{@code /nightvision <player>} — toggles it for another online player
 *       (requires {@code sessentials.nightvision.others}); the sender is told the
 *       outcome as well.</li>
 * </ul>
 *
 * <p>The effect is applied with an infinite duration and no ambient particles, screen
 * particles or HUD icon, so it stays visually clean. Applying or removing a potion
 * effect mutates the entity, so every toggle runs on the target player's Folia region
 * thread via {@link Schedulers#entity}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class NightVisionModule implements EssModule {

    @Override
    public String name() {
        return "nightvision";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("nightvision")
                .requires(s -> s.getSender().hasPermission("sessentials.nightvision"))
                .executes(Cmds.playerExec(player -> toggle(plugin, player, player)))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(Cmds.PLAYERS)
                        .requires(s -> s.getSender().hasPermission("sessentials.nightvision.others"))
                        .executes(ctx -> toggleOther(plugin, ctx)))
                .build(), "Toggle infinite night vision", List.of("nv")));
    }

    /**
     * Resolves the {@code <player>} argument and toggles night vision on that target.
     *
     * @param plugin the owning plugin
     * @param ctx    the command context holding the target name
     * @return 1 if a target was found and toggled, 0 if the target was offline
     */
    private static int toggleOther(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String targetName = StringArgumentType.getString(ctx, "player");

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            Msg.err(sender, targetName + " is not online.");
            return 0;
        }
        toggle(plugin, target, sender);
        return 1;
    }

    /**
     * Toggles infinite night vision on {@code target} from its region thread, messaging
     * the target and (when acting on someone else) the {@code sender}.
     *
     * @param plugin the owning plugin
     * @param target the player whose night vision is toggled
     * @param sender the command sender to confirm to when it differs from the target
     */
    private static void toggle(SEssentialsPlugin plugin, Player target, CommandSender sender) {
        boolean self = sender.equals(target);
        Schedulers.entity(plugin, target, () -> {
            boolean nowOn;
            if (target.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                target.removePotionEffect(PotionEffectType.NIGHT_VISION);
                nowOn = false;
            } else {
                target.addPotionEffect(new PotionEffect(
                        PotionEffectType.NIGHT_VISION, PotionEffect.INFINITE_DURATION, 0, false, false, false));
                nowOn = true;
            }
            if (nowOn) {
                Msg.ok(target, "Night vision on");
            } else {
                Msg.info(target, "Night vision off");
            }
            if (!self) {
                Msg.ok(sender, "Night vision " + (nowOn ? "on" : "off") + " for " + target.getName() + ".");
            }
        });
    }
}
