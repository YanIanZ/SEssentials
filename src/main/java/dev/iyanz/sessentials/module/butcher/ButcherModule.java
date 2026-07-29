package dev.iyanz.sessentials.module.butcher;

import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;

/**
 * Clears nearby mobs with {@code /butcher} (aliases {@code /killmobs}, {@code /mobkill}).
 *
 * <p>Usage: {@code /butcher [radius] [type]}. The command removes living entities within
 * {@code radius} blocks (default {@value #DEFAULT_RADIUS}) of the sender in their world.
 * Regardless of the chosen {@link ButcherType} it always spares a safety set — players,
 * tamed pets ({@link Tameable#isTamed()}), custom-named entities, and armor stands —
 * and it never touches non-living entities such as item frames, paintings or dropped
 * items (they are not {@link LivingEntity}). Villagers are additionally spared for every
 * category except {@link ButcherType#ALL}. The {@code type} argument narrows what is
 * removed: {@code monsters} (the default), {@code animals}, or {@code all}.</p>
 *
 * <p>Folia: the sweep runs inside the command executor, which is already the sender's
 * region thread, and it only reads and removes entities gathered from around the sender
 * ({@code getWorld().getNearbyEntities(getLocation(), ...)}) — the same region — so no
 * cross-region scheduler hop is required.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class ButcherModule implements EssModule {

    private static final String PERMISSION = "sessentials.butcher";

    /** Radius used when the command is run without an explicit radius argument. */
    private static final int DEFAULT_RADIUS = 100;

    /** Upper bound on the radius, guarding the region thread against oversized sweeps. */
    private static final int MAX_RADIUS = 512;

    @Override
    public String name() {
        return "butcher";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(
                Commands.literal("butcher")
                        .requires(s -> s.getSender().hasPermission(PERMISSION))
                        .executes(Cmds.playerExec(p -> sweep(p, DEFAULT_RADIUS, ButcherType.MONSTERS)))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, MAX_RADIUS))
                                .executes(ctx -> execute(ctx, ButcherType.MONSTERS))
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests(ButcherType.SUGGESTIONS)
                                        .executes(ButcherModule::executeTyped)))
                        .build(),
                "Remove nearby mobs (default monsters within " + DEFAULT_RADIUS + " blocks)",
                List.of("killmobs", "mobkill")));
    }

    /** Runs a sweep with a parsed radius and a fixed {@code type}. */
    private static int execute(CommandContext<CommandSourceStack> ctx, ButcherType type) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        sweep(player, IntegerArgumentType.getInteger(ctx, "radius"), type);
        return 1;
    }

    /** Runs a sweep with a parsed radius and a user-supplied type keyword. */
    private static int executeTyped(CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        String rawType = StringArgumentType.getString(ctx, "type");
        ButcherType type = ButcherType.parse(rawType);
        if (type == null) {
            Msg.err(player, "Unknown type \"" + rawType + "\". Use all, monsters, or animals.");
            return 0;
        }
        sweep(player, IntegerArgumentType.getInteger(ctx, "radius"), type);
        return 1;
    }

    /**
     * Removes every matching living entity within {@code radius} blocks of {@code player}
     * in their world and reports the count.
     *
     * @param player the sender to search around and to message
     * @param radius the cubic search radius in blocks
     * @param type   the category of entity to remove
     */
    private static void sweep(Player player, int radius, ButcherType type) {
        double r = radius;
        int removed = 0;
        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), r, r, r)) {
            if (entity instanceof LivingEntity living && isRemovable(living, type)) {
                living.remove();
                removed++;
            }
        }
        if (removed == 0) {
            Msg.info(player, "No " + type.noun(2) + " found within " + radius + " blocks.");
        } else {
            Msg.ok(player, "Removed " + removed + " " + type.noun(removed) + " within " + radius + " blocks.");
        }
    }

    /**
     * @param living the candidate living entity (already known to be non-null)
     * @param type   the requested category
     * @return {@code true} if the entity should be removed: it clears the safety filter
     *         (never a player, tamed pet, custom-named entity or armor stand; villagers
     *         only for {@link ButcherType#ALL}) and matches {@code type}
     */
    private static boolean isRemovable(LivingEntity living, ButcherType type) {
        if (living instanceof Player || living instanceof ArmorStand) {
            return false;
        }
        if (living instanceof Tameable tameable && tameable.isTamed()) {
            return false;
        }
        if (living.customName() != null) {
            return false;
        }
        if (living instanceof Villager && type != ButcherType.ALL) {
            return false;
        }
        return type.matches(living);
    }
}
