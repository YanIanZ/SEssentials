package dev.iyanz.sessentials.module.hpbar;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Listens for a player damaging a {@link LivingEntity} (directly, or via a projectile
 * they shot) and flashes a compact health readout for the victim in the attacker's
 * action bar.
 *
 * <p>Registered at {@link EventPriority#MONITOR} with {@code ignoreCancelled = true}
 * so it only reacts to damage that actually goes through, and never itself alters the
 * event. At MONITOR the victim's health has not yet been decremented by the engine,
 * so the post-hit health is computed from {@link EntityDamageByEntityEvent#getFinalDamage()}
 * rather than read back off the entity.</p>
 *
 * <p>Folia-safe: although this event always fires on the region thread shared by the
 * attacker and victim, the action bar is still dispatched via
 * {@link Schedulers#entity(org.bukkit.plugin.Plugin, Entity, Runnable)} to be
 * defensive about which of the two the current thread actually owns.</p>
 */
final class HpBarListener implements Listener {

    private final SEssentialsPlugin plugin;

    /**
     * @param plugin the owning plugin, used for config, scheduling and the opt-out store
     */
    HpBarListener(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Flashes the victim's post-hit health in the attacker's action bar. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!plugin.getConfig().getBoolean("hpbar.enabled", true)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }
        if (HpBarOptOut.isDisabled(plugin, attacker.getUniqueId())) {
            return;
        }

        AttributeInstance maxHealthAttribute = victim.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute == null) {
            return;
        }
        double maxHealth = maxHealthAttribute.getValue();
        double remainingHealth = Math.max(0.0, victim.getHealth() - event.getFinalDamage());

        Component actionBar = HpBarFormat.build(victim, remainingHealth, maxHealth);
        Schedulers.entity(plugin, attacker, () -> attacker.sendActionBar(actionBar));
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
}
