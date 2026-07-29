package dev.iyanz.sessentials.module.armorstand;

import java.util.function.Consumer;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Registers {@code /armorstand}: quick toggles ({@code arms}, {@code baseplate},
 * {@code gravity}, {@code size}, {@code visible}) and a {@code rotate <degrees>} command
 * for the nearest {@link ArmorStand} within {@value #RADIUS} blocks of the sender.
 *
 * <p>Finding the nearest stand only reads nearby entity locations, which is safe from
 * the sender's own region thread (mirrors how other report commands in this project
 * scan nearby entities). Every mutation of the stand itself, however, is dispatched to
 * the stand's own region thread via {@link Schedulers#entity}, since it need not be
 * owned by the same Folia region as the sender.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class ArmorStandModule implements EssModule {

    private static final String PERMISSION = "sessentials.armorstand";
    private static final double RADIUS = 5.0;

    @Override
    public String name() {
        return "armorstand";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(
                Commands.literal("armorstand")
                        .requires(s -> s.getSender().hasPermission(PERMISSION))
                        .executes(ctx -> {
                            Msg.err(ctx.getSource().getSender(),
                                    "Usage: /armorstand <arms|baseplate|gravity|size|visible|rotate>.");
                            return 0;
                        })
                        .then(Commands.literal("arms")
                                .executes(Cmds.playerExec(p -> apply(plugin, p, stand -> {
                                    boolean enabled = !stand.hasArms();
                                    stand.setArms(enabled);
                                    report(p, "Arms", enabled);
                                }))))
                        .then(Commands.literal("baseplate")
                                .executes(Cmds.playerExec(p -> apply(plugin, p, stand -> {
                                    boolean enabled = !stand.hasBasePlate();
                                    stand.setBasePlate(enabled);
                                    report(p, "Base plate", enabled);
                                }))))
                        .then(Commands.literal("gravity")
                                .executes(Cmds.playerExec(p -> apply(plugin, p, stand -> {
                                    boolean enabled = !stand.hasGravity();
                                    stand.setGravity(enabled);
                                    report(p, "Gravity", enabled);
                                }))))
                        .then(Commands.literal("size")
                                .executes(Cmds.playerExec(p -> apply(plugin, p, stand -> {
                                    boolean small = !stand.isSmall();
                                    stand.setSmall(small);
                                    Msg.ok(p, "Armor stand size set to " + (small ? "small" : "normal") + ".");
                                }))))
                        .then(Commands.literal("visible")
                                .executes(Cmds.playerExec(p -> apply(plugin, p, stand -> {
                                    boolean visible = !stand.isVisible();
                                    stand.setVisible(visible);
                                    report(p, "Visibility", visible);
                                }))))
                        .then(Commands.literal("rotate")
                                .then(Commands.argument("degrees", IntegerArgumentType.integer())
                                        .executes(ctx -> rotate(plugin, ctx))))
                        .build(),
                "Edit the nearest armor stand"));
    }

    /**
     * Finds the nearest armor stand to {@code player} (or errors if none is close
     * enough) and runs {@code action} on it, hopped to its own region thread.
     *
     * @param plugin the owning plugin, used to reach the stand's scheduler
     * @param player the sender to search around and to message
     * @param action the mutation to apply to the found stand
     */
    private static void apply(SEssentialsPlugin plugin, Player player, Consumer<ArmorStand> action) {
        ArmorStand stand = findNearest(player);
        if (stand == null) {
            Msg.err(player, "No armor stand within " + (int) RADIUS + " blocks.");
            return;
        }
        Schedulers.entity(plugin, stand, () -> action.accept(stand));
    }

    /** Sets the body yaw of the nearest armor stand to an absolute value, in degrees. */
    private static int rotate(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        int degrees = IntegerArgumentType.getInteger(ctx, "degrees");
        ArmorStand stand = findNearest(player);
        if (stand == null) {
            Msg.err(player, "No armor stand within " + (int) RADIUS + " blocks.");
            return 0;
        }
        Schedulers.entity(plugin, stand, () -> {
            float yaw = normalizeYaw(degrees);
            stand.setRotation(yaw, stand.getLocation().getPitch());
            Msg.ok(player, "Armor stand rotated to " + Math.round(yaw) + " degrees.");
        });
        return 1;
    }

    /** @return the closest {@link ArmorStand} to {@code player} within {@link #RADIUS} blocks, or {@code null}. */
    private static ArmorStand findNearest(Player player) {
        double radiusSquared = RADIUS * RADIUS;
        ArmorStand nearest = null;
        double nearestDistanceSquared = Double.MAX_VALUE;
        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), RADIUS, RADIUS, RADIUS)) {
            if (!(entity instanceof ArmorStand stand)) {
                continue;
            }
            double distanceSquared = stand.getLocation().distanceSquared(player.getLocation());
            if (distanceSquared <= radiusSquared && distanceSquared < nearestDistanceSquared) {
                nearest = stand;
                nearestDistanceSquared = distanceSquared;
            }
        }
        return nearest;
    }

    /** Normalizes {@code degrees} into Minecraft's yaw range, {@code [-180, 180)}. */
    private static float normalizeYaw(int degrees) {
        float yaw = degrees % 360;
        if (yaw >= 180f) {
            yaw -= 360f;
        } else if (yaw < -180f) {
            yaw += 360f;
        }
        return yaw;
    }

    /** Messages {@code player} that {@code feature} is now enabled or disabled. */
    private static void report(Player player, String feature, boolean enabled) {
        if (enabled) {
            Msg.ok(player, feature + " enabled.");
        } else {
            Msg.info(player, feature + " disabled.");
        }
    }
}
