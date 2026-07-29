package dev.iyanz.sessentials.module.fun;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.mojang.brigadier.arguments.StringArgumentType;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

/**
 * {@code /firework [player]}: launches a firework rocket with a randomly rolled
 * effect (type, colors, fade, flicker/trail) and power 1-2 at the sender, or an
 * optional {@code [player]} target. The entity spawn always runs on the target's
 * Folia region thread.
 */
@SuppressWarnings("UnstableApiUsage")
final class FireworkCommands {

    private static final int MIN_POWER = 1;
    private static final int MAX_POWER = 2;
    private static final int MIN_COLORS = 1;
    private static final int MAX_COLORS = 3;

    private FireworkCommands() {
    }

    /**
     * Registers {@code /firework} on {@code plugin}'s command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(
                Commands.literal("firework")
                        .requires(s -> s.getSender().hasPermission("sessentials.firework"))
                        .executes(Cmds.playerExec(p -> launch(plugin, p, p)))
                        .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                                .requires(s -> s.getSender().hasPermission("sessentials.firework.others"))
                                .executes(ctx -> {
                                    CommandSender sender = ctx.getSource().getSender();
                                    String name = StringArgumentType.getString(ctx, "player");
                                    Player target = Bukkit.getPlayerExact(name);
                                    if (target == null) {
                                        Msg.err(sender, name + " is not online.");
                                        return 0;
                                    }
                                    launch(plugin, sender, target);
                                    return 1;
                                }))
                        .build(),
                "Launch a random firework at yourself or a player"));
    }

    private static void launch(SEssentialsPlugin plugin, CommandSender sender, Player target) {
        Schedulers.entity(plugin, target, () -> {
            Location location = target.getLocation();
            target.getWorld().spawn(location, Firework.class, firework -> {
                FireworkMeta meta = firework.getFireworkMeta();
                meta.addEffect(randomEffect());
                meta.setPower(ThreadLocalRandom.current().nextInt(MIN_POWER, MAX_POWER + 1));
                firework.setFireworkMeta(meta);
            });
        });
        if (!target.equals(sender)) {
            Msg.ok(sender, "Launched a firework at " + target.getName() + ".");
        }
    }

    private static FireworkEffect randomEffect() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        FireworkEffect.Type[] types = FireworkEffect.Type.values();
        return FireworkEffect.builder()
                .with(types[random.nextInt(types.length)])
                .withColor(randomColors(random))
                .withFade(randomColors(random))
                .flicker(random.nextBoolean())
                .trail(random.nextBoolean())
                .build();
    }

    private static List<Color> randomColors(ThreadLocalRandom random) {
        DyeColor[] values = DyeColor.values();
        int count = MIN_COLORS + random.nextInt(MAX_COLORS - MIN_COLORS + 1);
        List<Color> colors = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            colors.add(values[random.nextInt(values.length)].getFireworkColor());
        }
        return colors;
    }
}
