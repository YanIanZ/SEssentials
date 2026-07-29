package dev.iyanz.sessentials.module.antispam;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Enforces two per-player chat anti-spam rules, reading its settings live from the plugin
 * config on every message so a {@code /antispam reload} takes effect immediately:
 *
 * <ul>
 *   <li><b>Cooldown</b> — if the player's previous accepted message was less than {@code
 *       anti-spam.cooldown-seconds} (default {@code 2}) ago, the new message is cancelled
 *       with a "chatting too fast" notice.</li>
 *   <li><b>Repeat block</b> — when {@code anti-spam.block-repeat} (default {@code true})
 *       is set, a message whose plain text equals the player's previous accepted message
 *       (case-insensitively) is cancelled with a "don't repeat yourself" notice.</li>
 * </ul>
 *
 * <p>The module is gated by {@code anti-spam.enabled} (default {@code true}), and players
 * holding {@code sessentials.antispam.bypass} are never limited. Both checks compare
 * against the last <em>accepted</em> message only: a blocked message updates neither the
 * stored timestamp nor the stored text, so the cooldown is measured from genuine chat and
 * a repeated line stays blocked no matter how often it is retried.</p>
 *
 * <p>Runs at {@link EventPriority#LOW} with {@code ignoreCancelled = true}, so it sits
 * after the moderation module's mute check (at {@link EventPriority#LOWEST}) — a muted
 * player's already-cancelled message is never counted — and sees the sender's original
 * plain text before this plugin's chat-colour/format listeners rewrite it. Folia-safe:
 * chat events already run off-region, the tracking maps are concurrent, and the only side
 * effects are cancelling the event and messaging the sender (both thread-safe).</p>
 */
public final class AntiSpamListener implements Listener {

    /** Permission that exempts a player from all anti-spam limiting. */
    private static final String BYPASS = "sessentials.antispam.bypass";

    /** Milliseconds in one second, used to convert the configured cooldown. */
    private static final long MILLIS_PER_SECOND = 1000L;

    private final SEssentialsPlugin plugin;

    /** Epoch-millis of each player's last accepted message. */
    private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();

    /** Plain text of each player's last accepted message. */
    private final Map<UUID, String> lastMessageText = new ConcurrentHashMap<>();

    /**
     * @param plugin the owning plugin, used for its live {@code anti-spam.*} config
     */
    public AntiSpamListener(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Applies the cooldown and repeat-block rules to an outgoing chat message. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("anti-spam.enabled", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission(BYPASS)) {
            return;
        }
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        long cooldownMillis = plugin.getConfig().getInt("anti-spam.cooldown-seconds", 2) * MILLIS_PER_SECOND;
        Long last = lastMessageTime.get(id);
        if (last != null && now - last < cooldownMillis) {
            event.setCancelled(true);
            Msg.err(player, "You're chatting too fast.");
            return;
        }

        String plain = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (plugin.getConfig().getBoolean("anti-spam.block-repeat", true)) {
            String previous = lastMessageText.get(id);
            if (previous != null && previous.equalsIgnoreCase(plain)) {
                event.setCancelled(true);
                Msg.err(player, "Please don't repeat yourself.");
                return;
            }
        }

        lastMessageText.put(id, plain);
        lastMessageTime.put(id, now);
    }

    /** Drops a player's tracked state on disconnect, keeping the maps bounded. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        lastMessageTime.remove(id);
        lastMessageText.remove(id);
    }
}
