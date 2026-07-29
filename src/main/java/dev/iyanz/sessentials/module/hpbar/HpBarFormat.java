package dev.iyanz.sessentials.module.hpbar;

import java.util.Locale;

import dev.iyanz.sessentials.util.SmallCaps;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.LivingEntity;

/**
 * Builds the compact action-bar health readout shown to an attacker:
 * {@code "<name> <hearts> current/max"}, where the hearts are a fixed-length bar
 * coloured by the remaining health fraction.
 */
final class HpBarFormat {

    /** Total number of heart glyphs in the bar, filled or not. */
    private static final int BAR_LENGTH = 10;
    private static final char HEART = '❤';

    /** Above this fraction the filled hearts are green. */
    private static final double GREEN_THRESHOLD = 0.6;
    /** Above this fraction (and at/below {@link #GREEN_THRESHOLD}) the filled hearts are yellow; at/below it, red. */
    private static final double YELLOW_THRESHOLD = 0.3;

    private HpBarFormat() {
    }

    /**
     * Builds the readout component for one hit.
     *
     * @param victim         the entity that was hit, used for its display name
     * @param currentHealth  the victim's health after this hit (already clamped to {@code >= 0})
     * @param maxHealth      the victim's max health ({@link org.bukkit.attribute.Attribute#MAX_HEALTH})
     * @return the assembled action-bar component
     */
    static Component build(LivingEntity victim, double currentHealth, double maxHealth) {
        double fraction = maxHealth > 0 ? clamp(currentHealth / maxHealth) : 0.0;
        int filled = clampSegments((int) Math.round(fraction * BAR_LENGTH));

        NamedTextColor barColor = fraction > GREEN_THRESHOLD ? NamedTextColor.GREEN
                : fraction > YELLOW_THRESHOLD ? NamedTextColor.YELLOW
                : NamedTextColor.RED;

        return Component.text()
                .append(displayName(victim))
                .append(Component.space())
                .append(Component.text(repeat(HEART, filled), barColor))
                .append(Component.text(repeat(HEART, BAR_LENGTH - filled), NamedTextColor.DARK_GRAY))
                .append(Component.space())
                .append(Component.text(displayHealth(currentHealth) + "/" + displayHealth(maxHealth), NamedTextColor.WHITE))
                .build();
    }

    /** @return the victim's custom name if set, otherwise its small-capped, title-cased entity type. */
    private static Component displayName(LivingEntity victim) {
        Component customName = victim.customName();
        if (customName != null) {
            return customName;
        }
        return Component.text(SmallCaps.of(titleCase(victim.getType().name())), NamedTextColor.WHITE);
    }

    /**
     * Rounds a health value to a whole number for display, without ever showing
     * {@code 0} while the entity still has any health remaining.
     *
     * @param health a health value {@code >= 0}
     * @return the rounded, non-decimal display value
     */
    private static long displayHealth(double health) {
        if (health <= 0.0) {
            return 0L;
        }
        return Math.max(1L, Math.round(health));
    }

    private static double clamp(double fraction) {
        return Math.max(0.0, Math.min(1.0, fraction));
    }

    private static int clampSegments(int segments) {
        return Math.max(0, Math.min(BAR_LENGTH, segments));
    }

    private static String repeat(char glyph, int count) {
        return String.valueOf(glyph).repeat(Math.max(0, count));
    }

    /**
     * @param enumName an upper-snake-case enum name, e.g. {@code "IRON_GOLEM"}
     * @return the same name in title case with spaces, e.g. {@code "Iron Golem"}
     */
    private static String titleCase(String enumName) {
        String[] parts = enumName.toLowerCase(Locale.ROOT).split("_");
        StringBuilder out = new StringBuilder(enumName.length());
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }
}
