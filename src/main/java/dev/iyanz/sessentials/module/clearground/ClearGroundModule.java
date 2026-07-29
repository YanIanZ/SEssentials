package dev.iyanz.sessentials.module.clearground;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Removes dropped {@link org.bukkit.entity.Item Item} entities lying on the ground of
 * a world, freeing up clutter that would otherwise persist until it despawns.
 *
 * <p>Commands (all gated by {@code sessentials.clearground}):</p>
 * <ul>
 *   <li>{@code /clearground [world]} (aliases {@code /cleardrops}, {@code /clearitems})
 *       broadcasts a warning and, ten seconds later, clears every dropped item in the
 *       target world, then broadcasts how many were removed.</li>
 *   <li>{@code /clearground now [world]} clears immediately, with no warning.</li>
 * </ul>
 *
 * <p>The target world defaults to the command sender's current world; a console sender
 * must always name a world explicitly. All entity/world work runs on Folia's
 * {@link org.bukkit.Bukkit#getGlobalRegionScheduler() global region scheduler} — see
 * {@link GroundCleaner} for the Folia-safe implementation.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class ClearGroundModule implements EssModule {

    /** Permission required for every {@code /clearground} variant. */
    private static final String PERMISSION = "sessentials.clearground";

    private SEssentialsPlugin plugin;

    @Override
    public String name() {
        return "clearground";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.plugin = plugin;
        plugin.commands(reg -> reg.register(
                Commands.literal("clearground")
                        .requires(s -> s.getSender().hasPermission(PERMISSION))
                        .executes(ctx -> warned(ctx, null))
                        .then(Commands.literal("now")
                                .executes(ctx -> immediate(ctx, null))
                                .then(Commands.argument("world", StringArgumentType.word())
                                        .suggests(GroundCleaner.WORLDS)
                                        .executes(ctx -> immediate(ctx,
                                                StringArgumentType.getString(ctx, "world")))))
                        .then(Commands.argument("world", StringArgumentType.word())
                                .suggests(GroundCleaner.WORLDS)
                                .executes(ctx -> warned(ctx,
                                        StringArgumentType.getString(ctx, "world"))))
                        .build(),
                "Clear dropped items lying on the ground",
                List.of("cleardrops", "clearitems")));
    }

    /** Warns the server, then clears the resolved world's dropped items after a delay. */
    private int warned(CommandContext<CommandSourceStack> ctx, String worldName) {
        CommandSender sender = ctx.getSource().getSender();
        World world = GroundCleaner.resolveWorld(sender, worldName);
        if (world == null) {
            return 0;
        }
        GroundCleaner.scheduleWarned(plugin, world);
        return 1;
    }

    /** Clears the resolved world's dropped items immediately, with no warning. */
    private int immediate(CommandContext<CommandSourceStack> ctx, String worldName) {
        CommandSender sender = ctx.getSource().getSender();
        World world = GroundCleaner.resolveWorld(sender, worldName);
        if (world == null) {
            return 0;
        }
        GroundCleaner.clearNow(plugin, world);
        return 1;
    }
}
