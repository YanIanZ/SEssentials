package dev.iyanz.sessentials.module.staffchat;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Holds the staff-chat channel's shared, in-memory state and performs delivery.
 *
 * <p>The only piece of state is the set of players currently in "staff-chat mode":
 * while a player is in that set, their normal chat is diverted to the staff channel
 * (see {@link StaffChatListener}) until they toggle the mode off again. The set is
 * process-local and never persisted; it is emptied naturally when the server stops.</p>
 *
 * <p>Delivery is deliberately safe against markup injection: the sender's message text
 * is always wrapped with {@link Component#text(String)} and is <strong>never</strong>
 * parsed as MiniMessage, so a player cannot smuggle colour tags, click events or similar
 * markup into the staff channel. Only the fixed {@code [Staff]} label and the sender's
 * name — which on vanilla servers is a short alphanumeric string — surround it.</p>
 */
final class StaffChatService {

    /** Permission required both to receive the channel and to speak into it. */
    static final String PERMISSION = "sessentials.staffchat";

    /** Players whose normal chat is currently routed to the staff channel. */
    private final Set<UUID> chatMode = ConcurrentHashMap.newKeySet();

    /**
     * Toggles staff-chat mode for the given player.
     *
     * @param playerId the player's unique id
     * @return {@code true} if the player is now in staff-chat mode, {@code false} if it
     *         was just turned off
     */
    boolean toggle(UUID playerId) {
        if (chatMode.remove(playerId)) {
            return false;
        }
        chatMode.add(playerId);
        return true;
    }

    /**
     * @param playerId the player's unique id
     * @return {@code true} if the player currently has staff-chat mode enabled
     */
    boolean inChatMode(UUID playerId) {
        return chatMode.contains(playerId);
    }

    /** Forgets any staff-chat-mode flag for the player (used on quit). */
    void clear(UUID playerId) {
        chatMode.remove(playerId);
    }

    /**
     * Broadcasts a message to the staff channel: the server console plus every online
     * player holding {@link #PERMISSION}, each messaged on their own region thread.
     *
     * <p>Safe to call from any thread (both the command executor's region thread and the
     * asynchronous chat-event thread): every player send is hopped onto the recipient's
     * {@code EntityScheduler}, and the console send is thread-safe.</p>
     *
     * @param plugin     the owning plugin, used for its schedulers and console sender
     * @param senderName the display name to attribute the message to
     * @param plainText  the raw, plain message text (rendered verbatim, never as markup)
     */
    void broadcast(SEssentialsPlugin plugin, String senderName, String plainText) {
        Component line = format(senderName, plainText);
        plugin.getServer().getConsoleSender().sendMessage(line);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission(PERMISSION)) {
                Schedulers.entity(plugin, online, () -> online.sendMessage(line));
            }
        }
    }

    /**
     * Builds the formatted staff-channel line: a teal {@code [Staff]} label, the
     * near-white sender name, a grey colon and finally the plain white message text.
     */
    private Component format(String senderName, String plainText) {
        return Msg.mm("<#43C6AC>[Staff] <#F5F5F5>" + senderName + "<#9AA0A6>: ")
                .append(Component.text(plainText, NamedTextColor.WHITE));
    }
}
