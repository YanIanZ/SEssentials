package dev.iyanz.sessentials.module.nicheextras;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Niche admin extras that act on every online player at once, plus a line-of-sight
 * teleport: {@code /ptimeall}, {@code /pweatherall}, {@code /extinguishall} and
 * {@code /jumpto}.
 *
 * <p>All bulk commands touch only per-player state (personal time/weather overrides,
 * fire ticks), and every mutation is hopped onto the owning player's region thread via
 * {@link dev.iyanz.sessentials.scheduler.Schedulers#entity}, so the module is
 * Folia-safe. {@code /jumpto} runs on the sender's own region thread and teleports
 * with {@code teleportAsync}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class NicheExtrasModule implements EssModule {

    @Override
    public String name() {
        return "nicheextras";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            reg.register(Commands.literal("ptimeall")
                    .requires(s -> s.getSender().hasPermission("sessentials.ptimeall"))
                    .then(Commands.argument("time", StringArgumentType.word())
                            .suggests((c, b) -> {
                                for (String s : new String[] {"day", "night", "noon", "midnight"}) {
                                    b.suggest(s);
                                }
                                return b.buildFuture();
                            })
                            .executes(ctx -> PersonalEnvBroadcast.timeAll(plugin,
                                    ctx.getSource().getSender(),
                                    StringArgumentType.getString(ctx, "time"))))
                    .build(), "Set every online player's personal time");

            reg.register(Commands.literal("pweatherall")
                    .requires(s -> s.getSender().hasPermission("sessentials.pweatherall"))
                    .then(Commands.argument("weather", StringArgumentType.word())
                            .suggests((c, b) -> {
                                for (String s : new String[] {"clear", "rain"}) {
                                    b.suggest(s);
                                }
                                return b.buildFuture();
                            })
                            .executes(ctx -> PersonalEnvBroadcast.weatherAll(plugin,
                                    ctx.getSource().getSender(),
                                    StringArgumentType.getString(ctx, "weather"))))
                    .build(), "Set every online player's personal weather");

            reg.register(Commands.literal("jumpto")
                    .requires(s -> s.getSender().hasPermission("sessentials.jumpto"))
                    .executes(Cmds.playerExec(JumpTeleport::jump))
                    .build(), "Teleport to the block you are looking at");

            reg.register(Commands.literal("extinguishall")
                    .requires(s -> s.getSender().hasPermission("sessentials.extinguishall"))
                    .executes(ctx -> FireExtinguisher.extinguishAll(plugin, ctx.getSource().getSender()))
                    .build(), "Put out fire on every online player");
        });
    }
}
