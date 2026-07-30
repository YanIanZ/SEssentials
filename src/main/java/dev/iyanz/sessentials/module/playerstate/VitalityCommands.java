package dev.iyanz.sessentials.module.playerstate;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/**
 * Vital-stat and movement commands: {@code /heal}, {@code /feed}, {@code /god},
 * {@code /fly} and {@code /speed}. Each acts on the sender by default, or on an
 * optional {@code [player]} target guarded by a {@code .others} permission.
 */
@SuppressWarnings("UnstableApiUsage")
final class VitalityCommands {

    private VitalityCommands() {
    }

    /**
     * Registers heal/feed/god/fly/speed on {@code reg}.
     *
     * @param reg        the command registrar
     * @param plugin     owning plugin (region-thread hops for target variants)
     * @param godService shared god-mode registry toggled by {@code /god}
     */
    static void register(Commands reg, SEssentialsPlugin plugin, GodService godService) {
        reg.register(
                CommandBuilders.selfOrTarget("heal", "sessentials.heal", "sessentials.heal.others", plugin,
                        VitalityCommands::heal),
                "Fully heal yourself or a player");

        reg.register(
                CommandBuilders.selfOrTarget("feed", "sessentials.feed", "sessentials.feed.others", plugin,
                        VitalityCommands::feed),
                "Refill your (or a player's) hunger and saturation");

        reg.register(
                CommandBuilders.selfOrTarget("god", "sessentials.god", "sessentials.god.others", plugin,
                        (sender, player) -> toggleGod(sender, player, godService, plugin)),
                "Toggle invulnerability for yourself or a player");

        reg.register(
                CommandBuilders.selfOrTarget("fly", "sessentials.fly", "sessentials.fly.others", plugin,
                        VitalityCommands::toggleFly),
                "Toggle flight for yourself or a player");

        registerSpeed(reg, plugin);
    }

    /** Fully heals {@code player}: max health, full hunger, and clears fire/poison/wither. */
    private static void heal(CommandSender sender, Player player) {
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        player.setHealth(maxHealth);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.removePotionEffect(PotionEffectType.POISON);
        player.removePotionEffect(PotionEffectType.WITHER);
        Msg.ok(player, "You have been healed.");
        if (sender != player) {
            Msg.ok(sender, "Healed " + player.getName() + ".");
        }
    }

    /** Refills {@code player}'s hunger bar and saturation. */
    private static void feed(CommandSender sender, Player player) {
        player.setFoodLevel(20);
        player.setSaturation(20f);
        Msg.ok(player, "You have been fed.");
        if (sender != player) {
            Msg.ok(sender, "Fed " + player.getName() + ".");
        }
    }

    private static void toggleGod(CommandSender sender, Player player, GodService godService, SEssentialsPlugin plugin) {
        boolean enabled = godService.toggle(player.getUniqueId());
        if (enabled) {
            Msg.ok(player, "God mode enabled.");
        } else {
            Msg.info(player, "God mode disabled.");
            // Clear both the runtime set (done by toggle above) and the persisted flag, and
            // drop the vanilla invulnerability flag a persisted-god restore may have set on
            // join, so /god off fully takes effect and never comes back on relog.
            player.setInvulnerable(false);
            if (GodPersistence.enabled(plugin)) {
                GodPersistence.clear(plugin, player.getUniqueId());
            }
        }
        if (sender != player) {
            Msg.ok(sender, player.getName() + "'s god mode is now " + (enabled ? "on" : "off") + ".");
        }
    }

    private static void toggleFly(CommandSender sender, Player player) {
        boolean enabled = !player.getAllowFlight();
        player.setAllowFlight(enabled);
        player.setFlying(enabled);
        if (enabled) {
            Msg.ok(player, "Flight enabled.");
        } else {
            Msg.info(player, "Flight disabled.");
        }
        if (sender != player) {
            Msg.ok(sender, player.getName() + "'s flight is now " + (enabled ? "on" : "off") + ".");
        }
    }

    private static void registerSpeed(Commands reg, SEssentialsPlugin plugin) {
        reg.register(
                Commands.literal("speed")
                        .requires(s -> s.getSender().hasPermission("sessentials.speed"))
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 10.0))
                                .executes(ctx -> {
                                    Player self = Cmds.player(ctx);
                                    if (self == null) {
                                        return 0;
                                    }
                                    double value = DoubleArgumentType.getDouble(ctx, "value");
                                    applySpeed(ctx.getSource().getSender(), self, value);
                                    return 1;
                                })
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .suggests(Cmds.PLAYERS)
                                        .requires(s -> s.getSender().hasPermission("sessentials.speed.others"))
                                        .executes(ctx -> {
                                            CommandSender sender = ctx.getSource().getSender();
                                            double value = DoubleArgumentType.getDouble(ctx, "value");
                                            String name = StringArgumentType.getString(ctx, "target");
                                            Player target = Bukkit.getPlayerExact(name);
                                            if (target == null) {
                                                Msg.err(sender, name + " is not online.");
                                                return 0;
                                            }
                                            Schedulers.entity(plugin, target, () -> applySpeed(sender, target, value));
                                            return 1;
                                        })))
                        .build(),
                "Set fly/walk speed (0-10)");
    }

    /** Maps {@code value} (0-10) onto Bukkit's -1..1 speed range and applies it. */
    private static void applySpeed(CommandSender sender, Player player, double value) {
        float mapped = (float) Math.max(-1.0, Math.min(1.0, value / 10.0));
        if (player.isFlying()) {
            player.setFlySpeed(mapped);
        } else {
            player.setWalkSpeed(mapped);
        }
        Msg.ok(player, "Speed set to " + value + ".");
        if (sender != player) {
            Msg.ok(sender, "Set " + player.getName() + "'s speed to " + value + ".");
        }
    }
}
