package dev.iyanz.sessentials.module.itemframe;

import java.util.function.Function;

import com.mojang.brigadier.context.CommandContext;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

/**
 * Builds and registers {@code /itemframe}, whose subcommands act on the {@link ItemFrame}
 * the sender is looking at:
 * <ul>
 *   <li>{@code rotate} — advance the item's rotation one step clockwise;</li>
 *   <li>{@code glow} — toggle the frame's glowing entity outline;</li>
 *   <li>{@code visible} — toggle whether the frame body itself is rendered (the item is
 *       always shown);</li>
 *   <li>{@code fixed} — toggle whether the frame is fixed (cannot be broken or interacted with).</li>
 * </ul>
 *
 * <p>The target frame is resolved by ray-tracing entities within {@value #RAY_DISTANCE}
 * blocks of the sender's eyes; blocks obstruct the ray so a frame behind a wall is not
 * matched. Every mutation is dispatched to the frame's Folia region thread via
 * {@link Schedulers#entity(org.bukkit.plugin.Plugin, org.bukkit.entity.Entity, Runnable)}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class ItemFrameCommands {

    /** Maximum reach, in blocks, when ray-tracing for the targeted item frame. */
    private static final int RAY_DISTANCE = 5;

    /** Permission required for every {@code /itemframe} subcommand. */
    private static final String PERMISSION = "sessentials.itemframe";

    private ItemFrameCommands() {
    }

    /**
     * Registers {@code /itemframe} on {@code plugin}'s command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(
                Commands.literal("itemframe")
                        .requires(s -> s.getSender().hasPermission(PERMISSION))
                        .then(Commands.literal("rotate")
                                .executes(ctx -> run(plugin, ctx, ItemFrameCommands::rotate)))
                        .then(Commands.literal("glow")
                                .executes(ctx -> run(plugin, ctx, ItemFrameCommands::glow)))
                        .then(Commands.literal("visible")
                                .executes(ctx -> run(plugin, ctx, ItemFrameCommands::visible)))
                        .then(Commands.literal("fixed")
                                .executes(ctx -> run(plugin, ctx, ItemFrameCommands::fixed)))
                        .build(),
                "Manage the item frame you are looking at"));
    }

    /**
     * Resolves the sender's targeted frame, then applies {@code op} on the frame's region
     * thread and reports the resulting state.
     *
     * @param plugin the owning plugin
     * @param ctx    the command context
     * @param op     mutates the frame and returns the plain-text message describing the new state
     * @return {@code 1} if a frame was found and updated, otherwise {@code 0}
     */
    private static int run(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx,
                           Function<ItemFrame, String> op) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        ItemFrame frame = frameInCrosshair(player);
        if (frame == null) {
            Msg.err(player, "You are not looking at an item frame.");
            return 0;
        }
        Schedulers.entity(plugin, frame, () -> Msg.ok(player, op.apply(frame)));
        return 1;
    }

    /**
     * @param player the looking player
     * @return the {@link ItemFrame} in the player's crosshair within {@value #RAY_DISTANCE}
     *         blocks, or {@code null} if none is targeted
     */
    private static ItemFrame frameInCrosshair(Player player) {
        RayTraceResult result = player.rayTraceEntities(RAY_DISTANCE, false);
        if (result != null && result.getHitEntity() instanceof ItemFrame frame) {
            return frame;
        }
        return null;
    }

    /** Advances the framed item's rotation one step clockwise. */
    private static String rotate(ItemFrame frame) {
        frame.setRotation(frame.getRotation().rotateClockwise());
        return "Rotated the item frame.";
    }

    /** Toggles the frame's glowing entity outline. */
    private static String glow(ItemFrame frame) {
        boolean glowing = !frame.isGlowing();
        frame.setGlowing(glowing);
        return glowing ? "The item frame is now glowing." : "The item frame is no longer glowing.";
    }

    /** Toggles whether the frame body itself is rendered (the item stays visible either way). */
    private static String visible(ItemFrame frame) {
        boolean visible = !frame.isVisible();
        frame.setVisible(visible);
        return visible ? "The item frame is now visible." : "The item frame is now invisible.";
    }

    /** Toggles whether the frame is fixed (cannot be broken or interacted with). */
    private static String fixed(ItemFrame frame) {
        boolean fixed = !frame.isFixed();
        frame.setFixed(fixed);
        return fixed ? "The item frame is now fixed." : "The item frame is no longer fixed.";
    }
}
