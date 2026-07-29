package dev.iyanz.sessentials.module.combat;

import java.util.Locale;
import java.util.Set;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * PvP combat tagging: tags both participants of a player-vs-player hit for
 * {@code combat.tag-seconds} (default {@code 15}), then restricts and punishes them
 * while tagged.
 *
 * <p>While a player is tagged, teleport-y commands ({@code /home}, {@code /tp},
 * {@code /spawn}, {@code /warp}, {@code /back}, {@code /tpa}) are blocked, and
 * disconnecting ("combat logging") kills them on the way out.</p>
 *
 * <p>Folia-safe: the damage event fires on the region thread shared by both
 * combatants, but messages are still dispatched via {@link Schedulers#entity} to be
 * defensive about which of the two the current thread actually owns; the
 * command-preprocess and quit events fire on the affected player's own region thread,
 * so those are handled in place.</p>
 */
final class CombatTagListener implements Listener {

    /** Command labels (including the leading slash) blocked while combat tagged. */
    private static final Set<String> BLOCKED_COMMANDS =
            Set.of("/home", "/tp", "/spawn", "/warp", "/back", "/tpa");

    private final SEssentialsPlugin plugin;
    private final CombatTag combatTag;

    /**
     * @param plugin    the owning plugin, used for config and scheduling
     * @param combatTag the shared combat-tag registry
     */
    CombatTagListener(SEssentialsPlugin plugin, CombatTag combatTag) {
        this.plugin = plugin;
        this.combatTag = combatTag;
    }

    /** Tags both participants when a player damages another player (directly or by projectile). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!plugin.getConfig().getBoolean("combat.enabled", true)) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        long seconds = plugin.getConfig().getLong("combat.tag-seconds", 15L);
        tagAndNotifyEnter(victim, seconds);
        tagAndNotifyEnter(attacker, seconds);
    }

    /** @return the player responsible for {@code damager} (itself, or its shooter if a projectile), or {@code null}. */
    private static Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    /** Tags {@code player} and, only on the untagged-to-tagged transition, messages them. */
    private void tagAndNotifyEnter(Player player, long seconds) {
        boolean entered = combatTag.tag(player.getUniqueId(), seconds);
        if (entered) {
            Schedulers.entity(plugin, player, () ->
                    Msg.err(player, "You are now in combat for " + seconds + " seconds. Logging out will kill you."));
        }
    }

    /** Blocks teleport-y commands while the sender is combat tagged. */
    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!combatTag.isTagged(player.getUniqueId())) {
            return;
        }
        String label = event.getMessage().split(" ", 2)[0].toLowerCase(Locale.ROOT);
        if (BLOCKED_COMMANDS.contains(label)) {
            event.setCancelled(true);
            Msg.err(player, "You can't do that in combat.");
        }
    }

    /** Punishes combat logging: a tagged player who disconnects dies on the way out. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (combatTag.isTagged(player.getUniqueId())) {
            combatTag.untag(player.getUniqueId());
            player.setHealth(0.0);
        }
    }
}
