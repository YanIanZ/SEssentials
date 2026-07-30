package dev.iyanz.sessentials.module.teleport;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.entity.Player;

/**
 * Shared post-teleport cooldown gate for the module's player-initiated teleports
 * ({@code /tp} self-move, {@code /tpaccept}, {@code /back}, {@code /spawn}). Distinct
 * from the {@code cmdwarmup} module's pre-teleport warmup: this enforces a minimum
 * number of seconds <em>between</em> a player's teleports.
 *
 * <p>Configured by {@code teleport.cooldown-seconds} in {@code config.yml}
 * (default {@value #DEFAULT_SECONDS} = disabled, making the gate a no-op). Players with
 * the {@code sessentials.teleport.cooldown.bypass} permission are exempt.</p>
 *
 * <p>Thread-safety: last-teleport stamps live in a {@link ConcurrentHashMap}, so the
 * gate may be consulted from any region thread. {@link #tryPass} must be called on the
 * region thread that owns the player being moved (it reads their permissions and
 * messages them); every call site in this module already runs there. Entries are
 * dropped on disconnect by {@link TeleportHistoryListener} so the map does not grow
 * without bound.</p>
 */
final class TeleportCooldown {

    /** {@code config.yml} key holding the cooldown length in seconds. */
    static final String CONFIG_KEY = "teleport.cooldown-seconds";
    /** Players with this permission are never held back by the cooldown. */
    static final String BYPASS_PERMISSION = "sessentials.teleport.cooldown.bypass";
    /** Default cooldown seconds when unset; {@code 0} disables the gate entirely. */
    static final int DEFAULT_SECONDS = 0;

    private final SEssentialsPlugin plugin;
    private final ConcurrentHashMap<UUID, Long> lastTeleportMillis = new ConcurrentHashMap<>();

    TeleportCooldown(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks whether {@code player} may teleport right now. If the cooldown is disabled,
     * the player holds the bypass permission, or enough time has passed since their last
     * gated teleport, their last-teleport time is stamped and the gate opens. Otherwise
     * the player is told how long to wait and the gate stays closed.
     *
     * <p>Call only once per command, immediately before performing the teleport, so a
     * command that aborts for other reasons (e.g. no {@code /back} history) never
     * consumes a stamp.</p>
     *
     * @param player the player about to be teleported (on their own region thread)
     * @return {@code true} to proceed with the teleport, {@code false} if still cooling down
     */
    boolean tryPass(Player player) {
        int cooldownSeconds = plugin.getConfig().getInt(CONFIG_KEY, DEFAULT_SECONDS);
        if (cooldownSeconds <= 0) {
            return true;
        }
        if (player.hasPermission(BYPASS_PERMISSION)) {
            return true;
        }
        long now = System.currentTimeMillis();
        Long last = lastTeleportMillis.get(player.getUniqueId());
        if (last != null) {
            long remainingMillis = last + cooldownSeconds * 1000L - now;
            if (remainingMillis > 0) {
                long remainingSeconds = (remainingMillis + 999L) / 1000L; // round up
                Msg.err(player, "Wait " + remainingSeconds + "s before teleporting again.");
                return false;
            }
        }
        lastTeleportMillis.put(player.getUniqueId(), now);
        return true;
    }

    /**
     * Forgets the player's last-teleport stamp. Called on disconnect so the per-player
     * map does not grow without bound.
     *
     * @param playerId the player's UUID
     */
    void remove(UUID playerId) {
        lastTeleportMillis.remove(playerId);
    }
}
