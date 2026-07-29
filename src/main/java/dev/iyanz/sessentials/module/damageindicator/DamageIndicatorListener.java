package dev.iyanz.sessentials.module.damageindicator;

import dev.iyanz.sessentials.SEssentialsPlugin;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for damage dealt to a {@link LivingEntity} and spawns a floating damage
 * number above the victim via {@link DamageIndicatorRenderer}.
 *
 * <p>Registered at {@link EventPriority#MONITOR} with {@code ignoreCancelled = true}
 * so it only reacts to damage that actually goes through and never itself alters the
 * event. The rounded {@linkplain EntityDamageEvent#getFinalDamage() final damage} is
 * used, so absorption/armor reductions are already accounted for.</p>
 *
 * <p>Folia-safe: {@link EntityDamageEvent} fires on the victim's region thread, which
 * is exactly the thread that owns the world/entity operations needed to spawn the
 * display, so the spawn is performed inline. The subsequent rise and removal are
 * scheduled on the display entity's <em>own</em> scheduler (see the renderer), never
 * the (absent-on-Folia) global {@code Bukkit.getScheduler()}.</p>
 */
final class DamageIndicatorListener implements Listener {

    private final SEssentialsPlugin plugin;
    private final DamageIndicatorOptOut optOut;

    /**
     * @param plugin the owning plugin, used for config and scheduling
     * @param optOut the in-memory per-player opt-out set
     */
    DamageIndicatorListener(SEssentialsPlugin plugin, DamageIndicatorOptOut optOut) {
        this.plugin = plugin;
        this.optOut = optOut;
    }

    /** Spawns a floating damage number above the freshly damaged victim. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!plugin.getConfig().getBoolean("damage-indicator.enabled", true)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        // Armor stands are LivingEntitys; skip them (and our own displays are not
        // LivingEntitys, so they never reach here).
        if (victim instanceof ArmorStand) {
            return;
        }
        double damage = Math.round(event.getFinalDamage() * 10.0) / 10.0;
        if (damage <= 0.0) {
            return;
        }
        if (victim instanceof Player player && optOut.isHidden(player.getUniqueId())) {
            return;
        }
        DamageIndicatorRenderer.spawn(plugin, victim, damage);
    }

    /** Drops a player's in-memory opt-out state when they leave. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        optOut.clear(event.getPlayer().getUniqueId());
    }
}
