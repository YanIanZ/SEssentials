package dev.iyanz.sessentials.module.damageindicator;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import dev.iyanz.sessentials.SEssentialsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Spawns and animates the short-lived {@link TextDisplay} entities that render floating
 * damage numbers.
 *
 * <p>A number is spawned just above the victim's eyes with a small random horizontal
 * and vertical jitter, so several near-simultaneous hits do not perfectly overlap. It
 * is a {@link Display.Billboard#CENTER center-billboarded} display with no background
 * and a slightly reduced scale, then it is nudged upward a couple of times and removed
 * after roughly one second for a rising, fading-away feel.</p>
 *
 * <p>Folia-safety: {@link #spawn} must be called on the victim's region thread (it is,
 * from the damage handler). All follow-up work — the upward nudges and the final
 * removal — is scheduled through the display entity's <em>own</em>
 * {@link org.bukkit.entity.Entity#getScheduler() entity scheduler}, so it always runs
 * on whichever region ends up owning the display; the legacy
 * {@code Bukkit.getScheduler()} is never used.</p>
 */
final class DamageIndicatorRenderer {

    /** At or above this (post-mitigation) damage the number is coloured as a heavy/crit hit. */
    private static final double CRIT_THRESHOLD = 8.0;
    /** Uniform display scale (slightly smaller than a full block of text). */
    private static final float SCALE = 0.9f;
    /** Vertical distance, in blocks, each nudge lifts the display. */
    private static final double RISE_STEP = 0.22;
    /** Tick offsets for the two upward nudges and the final removal. */
    private static final long RISE_ONE_TICKS = 7L;
    private static final long RISE_TWO_TICKS = 14L;
    private static final long REMOVE_TICKS = 20L;

    private DamageIndicatorRenderer() {
    }

    /**
     * Spawns a floating damage number above {@code victim}. Must be invoked on the
     * victim's region thread.
     *
     * @param plugin the owning plugin, used to schedule the rise/removal
     * @param victim the damaged entity the number floats above
     * @param damage the (already rounded, positive) damage to display
     */
    static void spawn(SEssentialsPlugin plugin, LivingEntity victim, double damage) {
        World world = victim.getWorld();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Location location = victim.getEyeLocation().add(
                (random.nextDouble() - 0.5) * 0.6,
                0.4 + random.nextDouble() * 0.25,
                (random.nextDouble() - 0.5) * 0.6);

        Component text = text(damage);
        TextDisplay display = world.spawn(location, TextDisplay.class, td -> configure(td, text));

        display.getScheduler().runDelayed(plugin, task -> raise(display), null, RISE_ONE_TICKS);
        display.getScheduler().runDelayed(plugin, task -> raise(display), null, RISE_TWO_TICKS);
        display.getScheduler().runDelayed(plugin, task -> display.remove(), null, REMOVE_TICKS);
    }

    /** Lifts the display a little. Runs on the display's own region thread. */
    private static void raise(TextDisplay display) {
        display.teleportAsync(display.getLocation().add(0.0, RISE_STEP, 0.0));
    }

    /** Applies the one-shot appearance of a damage number. Runs on the display's region thread. */
    private static void configure(TextDisplay display, Component text) {
        display.text(text);
        display.setBillboard(Display.Billboard.CENTER);
        display.setDefaultBackground(false);
        display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        display.setShadowed(false);
        display.setSeeThrough(true);
        display.setPersistent(false);
        display.setTransformation(new Transformation(
                new Vector3f(0.0f, 0.0f, 0.0f),
                new AxisAngle4f(0.0f, 0.0f, 0.0f, 1.0f),
                new Vector3f(SCALE, SCALE, SCALE),
                new AxisAngle4f(0.0f, 0.0f, 0.0f, 1.0f)));
    }

    /**
     * @param damage the rounded damage value
     * @return the {@code -N.N} label, red for a normal hit and gold for a heavy/crit hit
     *         (Bukkit exposes no reliable critical-hit flag, so magnitude is used as a proxy)
     */
    private static Component text(double damage) {
        String formatted = String.format(Locale.ROOT, "%.1f", damage);
        NamedTextColor color = damage >= CRIT_THRESHOLD ? NamedTextColor.GOLD : NamedTextColor.RED;
        return Component.text("-" + formatted, color);
    }
}
