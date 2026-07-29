package dev.iyanz.sessentials.module.armoreffects;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

/**
 * An immutable armor-effect rule read from one entry under
 * {@code armor-effects.effects} in {@code config.yml}: while any of a player's four
 * armor slots holds {@code material}, {@code effect} is (re)applied at {@code amplifier}.
 *
 * @param material  the armor item that must be worn (any of the four armor slots)
 * @param effect    the potion effect type granted while worn
 * @param amplifier the potion effect amplifier ({@code 0} = level I)
 */
record ArmorEffectRule(Material material, PotionEffectType effect, int amplifier) {
}
