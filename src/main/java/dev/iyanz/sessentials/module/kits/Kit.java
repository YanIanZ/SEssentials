package dev.iyanz.sessentials.module.kits;

import java.util.List;

import org.bukkit.inventory.ItemStack;

/**
 * An immutable kit definition: a set of items granted on {@code /kit <name>}, gated
 * by a per-kit cooldown.
 *
 * @param name            the kit's identifier (lower-case, as stored)
 * @param cooldownSeconds seconds a player must wait between successive uses of this kit
 * @param items           the items granted; callers must {@link ItemStack#clone() clone}
 *                        each before handing it to a player, since this list is the
 *                        shared, cached definition
 */
public record Kit(String name, int cooldownSeconds, List<ItemStack> items) {
}
