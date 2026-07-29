package dev.iyanz.sessentials.module.worldtime;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WeatherType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * World and personal time/weather commands: {@code /time}, {@code /day}, {@code /night},
 * {@code /weather} (+ {@code /sun}), {@code /ptime}, {@code /pweather}. World mutations
 * run on the world's region thread (Folia-safe).
 */
@SuppressWarnings("UnstableApiUsage")
public final class WorldTimeModule implements EssModule {

    private SEssentialsPlugin plugin;

    @Override
    public String name() {
        return "worldtime";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.plugin = plugin;
        plugin.commands(reg -> {
            reg.register(Commands.literal("time")
                    .requires(s -> s.getSender().hasPermission("sessentials.time"))
                    .then(Commands.argument("value", StringArgumentType.word())
                            .suggests((c, b) -> {
                                for (String s : new String[] {"day", "night", "noon", "midnight"}) {
                                    b.suggest(s);
                                }
                                return b.buildFuture();
                            })
                            .executes(ctx -> setTime(ctx.getSource().getSender(),
                                    StringArgumentType.getString(ctx, "value"))))
                    .build(), "Set the world time");

            reg.register(Commands.literal("day")
                    .requires(s -> s.getSender().hasPermission("sessentials.time"))
                    .executes(ctx -> setTime(ctx.getSource().getSender(), "day")).build(), "Set time to day");
            reg.register(Commands.literal("night")
                    .requires(s -> s.getSender().hasPermission("sessentials.time"))
                    .executes(ctx -> setTime(ctx.getSource().getSender(), "night")).build(), "Set time to night");

            reg.register(Commands.literal("weather")
                    .requires(s -> s.getSender().hasPermission("sessentials.weather"))
                    .then(Commands.argument("type", StringArgumentType.word())
                            .suggests((c, b) -> {
                                for (String s : new String[] {"clear", "rain", "storm"}) {
                                    b.suggest(s);
                                }
                                return b.buildFuture();
                            })
                            .executes(ctx -> setWeather(ctx.getSource().getSender(),
                                    StringArgumentType.getString(ctx, "type"))))
                    .build(), "Set the world weather");
            reg.register(Commands.literal("sun")
                    .requires(s -> s.getSender().hasPermission("sessentials.weather"))
                    .executes(ctx -> setWeather(ctx.getSource().getSender(), "clear")).build(), "Clear the weather");

            reg.register(Commands.literal("ptime")
                    .requires(s -> s.getSender().hasPermission("sessentials.ptime"))
                    .then(Commands.argument("value", StringArgumentType.word())
                            .suggests((c, b) -> {
                                for (String s : new String[] {"day", "night", "reset"}) {
                                    b.suggest(s);
                                }
                                return b.buildFuture();
                            })
                            .executes(ctx -> {
                                Player p = Cmds.player(ctx);
                                if (p == null) {
                                    return 0;
                                }
                                return setPersonalTime(p, StringArgumentType.getString(ctx, "value"));
                            }))
                    .build(), "Set your personal time");

            reg.register(Commands.literal("pweather")
                    .requires(s -> s.getSender().hasPermission("sessentials.pweather"))
                    .then(Commands.argument("value", StringArgumentType.word())
                            .suggests((c, b) -> {
                                for (String s : new String[] {"clear", "rain", "reset"}) {
                                    b.suggest(s);
                                }
                                return b.buildFuture();
                            })
                            .executes(ctx -> {
                                Player p = Cmds.player(ctx);
                                if (p == null) {
                                    return 0;
                                }
                                return setPersonalWeather(p, StringArgumentType.getString(ctx, "value"));
                            }))
                    .build(), "Set your personal weather");
        });
    }

    private int setTime(CommandSender sender, String value) {
        if (!(sender instanceof Player player)) {
            Msg.err(sender, "Run this in-game (world context needed).");
            return 0;
        }
        long ticks;
        switch (value.toLowerCase()) {
            case "day" -> ticks = 1000L;
            case "night" -> ticks = 13000L;
            case "noon" -> ticks = 6000L;
            case "midnight" -> ticks = 18000L;
            default -> {
                try {
                    ticks = Long.parseLong(value);
                } catch (NumberFormatException ex) {
                    Msg.err(sender, "Unknown time: " + value);
                    return 0;
                }
            }
        }
        World world = player.getWorld();
        long t = ticks;
        // On Folia the day-time is GLOBAL-region state (not owned by any chunk region),
        // so setTime must run on the global region thread — the per-region scheduler
        // throws "Cannot modify time off of the global region".
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> world.setTime(t));
        Msg.value(sender, "Time set to", value);
        return 1;
    }

    private int setWeather(CommandSender sender, String type) {
        if (!(sender instanceof Player player)) {
            Msg.err(sender, "Run this in-game (world context needed).");
            return 0;
        }
        World world = player.getWorld();
        boolean storm;
        boolean thunder;
        switch (type.toLowerCase()) {
            case "clear", "sun" -> {
                storm = false;
                thunder = false;
            }
            case "rain" -> {
                storm = true;
                thunder = false;
            }
            case "storm", "thunder" -> {
                storm = true;
                thunder = true;
            }
            default -> {
                Msg.err(sender, "Unknown weather: " + type);
                return 0;
            }
        }
        // Weather is also GLOBAL-region state on Folia — must run on the global thread.
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            world.setStorm(storm);
            world.setThundering(thunder);
        });
        Msg.value(sender, "Weather set to", type);
        return 1;
    }

    private int setPersonalTime(Player player, String value) {
        switch (value.toLowerCase()) {
            case "reset" -> {
                player.resetPlayerTime();
                Msg.ok(player, "Personal time reset.");
            }
            case "day" -> {
                player.setPlayerTime(1000L, false);
                Msg.ok(player, "Personal time set to day.");
            }
            case "night" -> {
                player.setPlayerTime(13000L, false);
                Msg.ok(player, "Personal time set to night.");
            }
            default -> {
                try {
                    player.setPlayerTime(Long.parseLong(value), false);
                    Msg.value(player, "Personal time set to", value);
                } catch (NumberFormatException ex) {
                    Msg.err(player, "Unknown time: " + value);
                    return 0;
                }
            }
        }
        return 1;
    }

    private int setPersonalWeather(Player player, String value) {
        switch (value.toLowerCase()) {
            case "reset" -> {
                player.resetPlayerWeather();
                Msg.ok(player, "Personal weather reset.");
            }
            case "clear", "sun" -> {
                player.setPlayerWeather(WeatherType.CLEAR);
                Msg.ok(player, "Personal weather set to clear.");
            }
            case "rain", "storm" -> {
                player.setPlayerWeather(WeatherType.DOWNFALL);
                Msg.ok(player, "Personal weather set to rain.");
            }
            default -> {
                Msg.err(player, "Unknown weather: " + value);
                return 0;
            }
        }
        return 1;
    }
}
