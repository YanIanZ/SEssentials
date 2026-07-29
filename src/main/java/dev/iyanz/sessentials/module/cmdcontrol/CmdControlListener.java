package dev.iyanz.sessentials.module.cmdcontrol;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Enforces per-command cooldowns and Vault costs on {@link PlayerCommandPreprocessEvent},
 * per the rules held in {@link CmdControlSettings}.
 *
 * <p>Enforcement order per event: a command with no configured rule (or a bypassing
 * player) is left untouched; otherwise the cooldown is checked first (a player still on
 * cooldown is never charged), then the cost is charged (a player who can't afford it is
 * never put on cooldown), and only once both checks pass does the cooldown for the next
 * use start. The event itself is never cancelled unless the player is blocked.</p>
 *
 * <p>Cooldowns are tracked purely in memory (per player, per command label) and reset on
 * restart; a player's cooldown state is dropped on disconnect to bound memory use.</p>
 *
 * <p>Folia-safe: {@link PlayerCommandPreprocessEvent} fires on the sending player's own
 * region thread, so the Vault economy call here always runs on that thread already —
 * no scheduler hop is needed.</p>
 */
final class CmdControlListener implements Listener {

    private static final String GLOBAL_BYPASS_PERMISSION = "sessentials.cmdcontrol.bypass";

    private final SEssentialsPlugin plugin;
    private final CmdControlSettings settings;

    /** Next-allowed-use epoch millis, keyed by player UUID then by command label. */
    private final Map<UUID, Map<String, Long>> nextAvailable = new ConcurrentHashMap<>();

    /**
     * @param plugin   the owning plugin, used for the Vault economy hook
     * @param settings the shared, reloadable rule set to enforce
     */
    CmdControlListener(SEssentialsPlugin plugin, CmdControlSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    /** Applies the configured cooldown/cost rule (if any) to the command being run. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!settings.enabled()) {
            return;
        }
        String label = label(event.getMessage());
        CommandRule rule = settings.rule(label);
        if (rule == null) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission(GLOBAL_BYPASS_PERMISSION) || player.hasPermission(rule.bypassPermission())) {
            return;
        }

        UUID uuid = player.getUniqueId();
        Map<String, Long> playerCooldowns = nextAvailable.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());

        long now = System.currentTimeMillis();
        Long readyAt = playerCooldowns.get(label);
        if (readyAt != null && readyAt > now) {
            event.setCancelled(true);
            Msg.err(player, "You must wait " + CooldownFormat.compact(readyAt - now) + " to use that again.");
            return;
        }

        if (rule.cost() > 0.0 && plugin.economy().available()) {
            if (!plugin.economy().has(uuid, rule.cost())) {
                event.setCancelled(true);
                Msg.err(player, "You need " + plugin.economy().format(rule.cost()) + " to use that command.");
                return;
            }
            plugin.economy().withdraw(uuid, rule.cost());
        }

        if (rule.cooldownSeconds() > 0L) {
            playerCooldowns.put(label, now + rule.cooldownSeconds() * 1000L);
        }
    }

    /** Drops a disconnecting player's cooldown entries so memory use stays bounded. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        nextAvailable.remove(event.getPlayer().getUniqueId());
    }

    /** @return the lowercase command label (no leading slash), parsed from the raw preprocess message. */
    private static String label(String rawMessage) {
        String firstToken = rawMessage.split(" ", 2)[0];
        String withoutSlash = firstToken.startsWith("/") ? firstToken.substring(1) : firstToken;
        return withoutSlash.toLowerCase(Locale.ROOT);
    }
}
