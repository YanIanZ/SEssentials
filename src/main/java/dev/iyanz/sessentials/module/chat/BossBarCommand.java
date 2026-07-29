package dev.iyanz.sessentials.module.chat;

import java.util.concurrent.TimeUnit;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Registers {@code /bossbar <player> <text...>}: shows a temporary boss bar to a
 * player, automatically hiding it again after a few seconds. The text is
 * operator-supplied and parsed as MiniMessage; both the initial show and the later
 * hide run on the target's own region thread (Folia-safe).
 */
@SuppressWarnings("UnstableApiUsage")
public final class BossBarCommand {

    private static final long DURATION_SECONDS = 5L;

    private BossBarCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    public static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            var node = Commands.literal("bossbar")
                    .requires(s -> s.getSender().hasPermission("sessentials.bossbar"))
                    .then(Commands.argument("player", StringArgumentType.word())
                            .suggests(Cmds.PLAYERS)
                            .then(Commands.argument("text", StringArgumentType.greedyString())
                                    .executes(ctx -> send(plugin, ctx))))
                    .build();
            reg.register(node, "Show a temporary boss bar to a player");
        });
    }

    private static int send(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Msg.err(sender, name + " is not online.");
            return 0;
        }
        String text = StringArgumentType.getString(ctx, "text");
        Component message = Msg.mm(text);
        BossBar bar = BossBar.bossBar(message, 1.0f, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);

        Schedulers.entity(plugin, target, () -> target.showBossBar(bar));
        Bukkit.getAsyncScheduler().runDelayed(plugin,
                scheduled -> Schedulers.entity(plugin, target, () -> target.hideBossBar(bar)),
                DURATION_SECONDS, TimeUnit.SECONDS);

        Msg.ok(sender, "Sent a boss bar to " + target.getName() + ".");
        return 1;
    }
}
