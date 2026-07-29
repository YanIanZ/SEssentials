package dev.iyanz.sessentials.module.utility;

import java.util.Locale;

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
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

/**
 * Miscellaneous utility commands: {@code near}, {@code list}, {@code sudo},
 * {@code spawnmob}, {@code lightning} and {@code gc}. ({@code ping} and {@code tps}
 * are deliberately excluded.)
 */
@SuppressWarnings("UnstableApiUsage")
public final class UtilityModule implements EssModule {

    private SEssentialsPlugin plugin;

    @Override
    public String name() {
        return "utility";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.plugin = plugin;
        plugin.commands(reg -> {
            reg.register(Commands.literal("near")
                    .requires(s -> s.getSender().hasPermission("sessentials.near"))
                    .executes(Cmds.playerExec(p -> near(p, 100)))
                    .then(Commands.argument("radius", IntegerArgumentType.integer(1, 5000))
                            .executes(ctx -> {
                                Player p = Cmds.player(ctx);
                                if (p == null) {
                                    return 0;
                                }
                                near(p, IntegerArgumentType.getInteger(ctx, "radius"));
                                return 1;
                            }))
                    .build(), "List nearby players", java.util.List.of("nearby"));

            reg.register(Commands.literal("list")
                    .requires(s -> s.getSender().hasPermission("sessentials.list"))
                    .executes(this::list)
                    .build(), "List online players", java.util.List.of("online", "who"));

            reg.register(Commands.literal("sudo")
                    .requires(s -> s.getSender().hasPermission("sessentials.sudo"))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .then(Commands.argument("command", StringArgumentType.greedyString())
                                    .executes(this::sudo)))
                    .build(), "Make a player run a command");

            reg.register(Commands.literal("spawnmob")
                    .requires(s -> s.getSender().hasPermission("sessentials.spawnmob"))
                    .then(Commands.argument("type", StringArgumentType.word()).suggests(this::mobSuggestions)
                            .executes(ctx -> spawnMob(ctx, 1))
                            .then(Commands.argument("amount", IntegerArgumentType.integer(1, 50))
                                    .executes(ctx -> spawnMob(ctx, IntegerArgumentType.getInteger(ctx, "amount")))))
                    .build(), "Spawn mobs", java.util.List.of("mob"));

            reg.register(Commands.literal("lightning")
                    .requires(s -> s.getSender().hasPermission("sessentials.lightning"))
                    .executes(Cmds.playerExec(p -> strike(p)))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .executes(ctx -> {
                                Player t = Bukkit.getPlayerExact(StringArgumentType.getString(ctx, "player"));
                                if (t == null) {
                                    Msg.err(ctx.getSource().getSender(), "That player is not online.");
                                    return 0;
                                }
                                strike(t);
                                return 1;
                            }))
                    .build(), "Strike lightning", java.util.List.of("strike"));

            reg.register(Commands.literal("gc")
                    .requires(s -> s.getSender().hasPermission("sessentials.gc"))
                    .executes(ctx -> {
                        Runtime rt = Runtime.getRuntime();
                        long used = (rt.totalMemory() - rt.freeMemory()) / 1_048_576;
                        long max = rt.maxMemory() / 1_048_576;
                        Msg.value(ctx.getSource().getSender(), "Memory:", used + " / " + max + " MB");
                        return 1;
                    })
                    .build(), "Show memory usage", java.util.List.of("memory"));
        });
    }

    private void near(Player player, int radius) {
        double r2 = (double) radius * radius;
        Location origin = player.getLocation();
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player)) {
                continue;
            }
            double d2 = other.getLocation().distanceSquared(origin);
            if (d2 <= r2) {
                if (count > 0) {
                    sb.append("<#9AA0A6>, ");
                }
                sb.append("<#F5F5F5>").append(other.getName())
                        .append(" <#9AA0A6>(").append((int) Math.sqrt(d2)).append("m)");
                count++;
            }
        }
        if (count == 0) {
            Msg.info(player, "No players within " + radius + " blocks.");
        } else {
            Msg.raw(player, Msg.prefix() + "<#9AA0A6>" + dev.iyanz.sessentials.util.SmallCaps.of("Nearby: ") + sb);
        }
    }

    private int list(CommandContext<CommandSourceStack> ctx) {
        var players = Bukkit.getOnlinePlayers();
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Player p : players) {
            if (!first) {
                sb.append("<#9AA0A6>, ");
            }
            sb.append("<#F5F5F5>").append(p.getName());
            first = false;
        }
        Msg.value(ctx.getSource().getSender(), "Online (" + players.size() + "):", "");
        Msg.raw(ctx.getSource().getSender(), sb.toString());
        return 1;
    }

    private int sudo(CommandContext<CommandSourceStack> ctx) {
        Player t = Bukkit.getPlayerExact(StringArgumentType.getString(ctx, "player"));
        if (t == null) {
            Msg.err(ctx.getSource().getSender(), "That player is not online.");
            return 0;
        }
        String command = StringArgumentType.getString(ctx, "command");
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        Schedulers.entity(plugin, t, () -> t.performCommand(cmd));
        Msg.ok(ctx.getSource().getSender(), "Sudo sent to " + t.getName() + ".");
        return 1;
    }

    private java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> mobSuggestions(
            CommandContext<CommandSourceStack> ctx, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();
        for (EntityType type : EntityType.values()) {
            if (type.isSpawnable() && type.name().toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(type.name().toLowerCase(Locale.ROOT));
            }
        }
        return builder.buildFuture();
    }

    private int spawnMob(CommandContext<CommandSourceStack> ctx, int amount) {
        Player p = Cmds.player(ctx);
        if (p == null) {
            return 0;
        }
        EntityType type;
        try {
            type = EntityType.valueOf(StringArgumentType.getString(ctx, "type").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            Msg.err(p, "Unknown mob type.");
            return 0;
        }
        if (!type.isSpawnable()) {
            Msg.err(p, "That entity can't be spawned.");
            return 0;
        }
        Location loc = p.getLocation();
        int n = Math.min(50, amount);
        Schedulers.entity(plugin, p, () -> {
            for (int i = 0; i < n; i++) {
                p.getWorld().spawnEntity(loc, type);
            }
        });
        Msg.ok(p, "Spawned " + n + " " + type.name().toLowerCase(Locale.ROOT) + ".");
        return 1;
    }

    private void strike(Player target) {
        Schedulers.entity(plugin, target, () -> target.getWorld().strikeLightning(target.getLocation()));
    }
}
