package dev.iyanz.sessentials.module.healall;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Heal-all module: {@code /healall} restores every online player on the server to full
 * health and full hunger in one command.
 *
 * <p>Behaviour:</p>
 * <ul>
 *   <li>{@code /healall} — sets each online player's health to their
 *       {@link Attribute#MAX_HEALTH} value and their food level to {@value #FULL_FOOD}
 *       (requires {@code sessentials.healall}); works from console too.</li>
 * </ul>
 *
 * <p>Health and food are entity state, so on Folia each player is healed on their own
 * region thread via {@link Schedulers#entity}. The sender is told how many players the
 * heal was dispatched to.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class HealAllModule implements EssModule {

    /** The vanilla maximum food level. */
    private static final int FULL_FOOD = 20;

    @Override
    public String name() {
        return "healall";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("healall")
                .requires(s -> s.getSender().hasPermission("sessentials.healall"))
                .executes(ctx -> healAll(plugin, ctx.getSource().getSender()))
                .build(), "Heal every online player"));
    }

    /**
     * Schedules a full heal for every online player, each on that player's region
     * thread, and confirms the dispatched count to the sender.
     *
     * @param plugin the owning plugin
     * @param sender the command sender to confirm to
     * @return 1 always (the command itself cannot fail)
     */
    private static int healAll(SEssentialsPlugin plugin, CommandSender sender) {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            Schedulers.entity(plugin, player, () -> heal(player));
            count++;
        }
        Msg.ok(sender, "Healed " + count + (count == 1 ? " player." : " players."));
        return 1;
    }

    /**
     * Restores one player to full health and full hunger. Must run on the player's
     * region thread.
     *
     * @param player the player to heal
     */
    private static void heal(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            player.setHealth(maxHealth.getValue());
        }
        player.setFoodLevel(FULL_FOOD);
        Msg.info(player, "You were healed.");
    }
}
