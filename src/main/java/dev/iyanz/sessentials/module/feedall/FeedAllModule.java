package dev.iyanz.sessentials.module.feedall;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Feed-all module: {@code /feedall} refills every online player's hunger and
 * saturation in one command.
 *
 * <p>Behaviour:</p>
 * <ul>
 *   <li>{@code /feedall} — sets each online player's food level to {@value #FULL_FOOD}
 *       and saturation to {@value #FULL_SATURATION}
 *       (requires {@code sessentials.feedall}); works from console too.</li>
 * </ul>
 *
 * <p>Food and saturation are entity state, so on Folia each player is fed on their own
 * region thread via {@link Schedulers#entity}. The sender is told how many players the
 * feed was dispatched to.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class FeedAllModule implements EssModule {

    /** The vanilla maximum food level. */
    private static final int FULL_FOOD = 20;
    /** Saturation applied alongside the full food level. */
    private static final float FULL_SATURATION = 20.0f;

    @Override
    public String name() {
        return "feedall";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("feedall")
                .requires(s -> s.getSender().hasPermission("sessentials.feedall"))
                .executes(ctx -> feedAll(plugin, ctx.getSource().getSender()))
                .build(), "Feed every online player"));
    }

    /**
     * Schedules a full feed for every online player, each on that player's region
     * thread, and confirms the dispatched count to the sender.
     *
     * @param plugin the owning plugin
     * @param sender the command sender to confirm to
     * @return 1 always (the command itself cannot fail)
     */
    private static int feedAll(SEssentialsPlugin plugin, CommandSender sender) {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            Schedulers.entity(plugin, player, () -> feed(player));
            count++;
        }
        Msg.ok(sender, "Fed " + count + (count == 1 ? " player." : " players."));
        return 1;
    }

    /**
     * Refills one player's hunger and saturation. Must run on the player's region
     * thread.
     *
     * @param player the player to feed
     */
    private static void feed(Player player) {
        player.setFoodLevel(FULL_FOOD);
        player.setSaturation(FULL_SATURATION);
        Msg.info(player, "You were fed.");
    }
}
