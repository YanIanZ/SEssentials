package dev.iyanz.sessentials.selib.effect;

import dev.iyanz.sessentials.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Helpers for the action bar (the text above the hotbar). {@link Player#sendActionBar(Component)}
 * is a thread-safe Adventure call, so no region-thread hop is needed.
 */
public final class ActionBars {

    private ActionBars() {
    }

    /**
     * Sends a component to the player's action bar.
     *
     * @param player  the recipient
     * @param message the message component
     */
    public static void send(Player player, Component message) {
        player.sendActionBar(message);
    }

    /**
     * Sends a MiniMessage (legacy codes accepted) string to the player's action bar.
     *
     * @param player      the recipient
     * @param miniMessage the message in MiniMessage form
     */
    public static void send(Player player, String miniMessage) {
        player.sendActionBar(Msg.mm(miniMessage));
    }
}
