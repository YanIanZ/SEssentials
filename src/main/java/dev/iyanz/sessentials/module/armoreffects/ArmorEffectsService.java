package dev.iyanz.sessentials.module.armoreffects;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

/**
 * Applies configured {@link ArmorEffectRule}s to a single player.
 *
 * <p><b>Folia:</b> every call must already be running on that player's own region
 * thread — callers hop there via
 * {@link dev.iyanz.sessentials.scheduler.Schedulers#entity} before invoking
 * {@link #apply}, since both reading armor contents and granting potion effects are
 * entity-affecting operations.</p>
 */
final class ArmorEffectsService {

    /**
     * Duration, in ticks, each grant lasts. Deliberately short and re-applied every
     * scheduler cycle so the effect fades out shortly after the armor piece is
     * removed instead of lingering (3 seconds &gt; the ~2-second sweep period, so
     * a worn piece never visibly flickers between grants).
     */
    private static final int EFFECT_DURATION_TICKS = 60;

    private ArmorEffectsService() {
    }

    /**
     * Reads {@code player}'s currently worn armor and (re)applies the potion effect of
     * every rule whose material is worn in any armor slot.
     *
     * @param player the player to inspect and affect; must already be on this
     *               player's region thread
     * @param rules  the configured armor-effect rules
     */
    static void apply(Player player, List<ArmorEffectRule> rules) {
        if (rules.isEmpty()) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        ItemStack[] armorContents = inventory.getArmorContents();

        for (ArmorEffectRule rule : rules) {
            if (isWorn(armorContents, rule.material())) {
                player.addPotionEffect(new PotionEffect(
                        rule.effect(), EFFECT_DURATION_TICKS, rule.amplifier(), true, false));
            }
        }
    }

    /** @return whether any slot in {@code armorContents} holds {@code material}. */
    private static boolean isWorn(ItemStack[] armorContents, Material material) {
        for (ItemStack piece : armorContents) {
            if (piece != null && piece.getType() == material) {
                return true;
            }
        }
        return false;
    }
}
