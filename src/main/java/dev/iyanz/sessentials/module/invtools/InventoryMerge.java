package dev.iyanz.sessentials.module.invtools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.bukkit.inventory.ItemStack;

/**
 * Small, self-contained stack-merging helper shared by {@code /sortinv} and
 * {@code /stack}: groups a slot range's items by {@link ItemStack#isSimilar}, then
 * lays each group back out as full stacks (a final partial stack, if any, last).
 */
final class InventoryMerge {

    private InventoryMerge() {
    }

    /**
     * Merges same-item stacks in {@code contents}, keeping groups in the order their
     * item first appeared. The returned array is the same length as {@code contents}.
     *
     * @param contents the slots to merge (nulls/air are treated as empty)
     * @return a freshly built array of merged, full-sized stacks
     */
    static ItemStack[] merge(ItemStack[] contents) {
        return layOut(group(contents), contents.length);
    }

    /**
     * Merges same-item stacks in {@code contents}, then additionally sorts the
     * resulting groups alphabetically by material name.
     *
     * @param contents the slots to merge and sort (nulls/air are treated as empty)
     * @return a freshly built array of merged, sorted, full-sized stacks
     */
    static ItemStack[] mergeSorted(ItemStack[] contents) {
        List<ItemStack> groups = group(contents);
        groups.sort(Comparator.comparing(stack -> stack.getType().name()));
        return layOut(groups, contents.length);
    }

    /** Groups {@code contents} into one representative stack per distinct item, amounts summed. */
    private static List<ItemStack> group(ItemStack[] contents) {
        List<ItemStack> representatives = new ArrayList<>();
        List<Integer> totals = new ArrayList<>();
        for (ItemStack stack : contents) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            int bucket = -1;
            for (int i = 0; i < representatives.size(); i++) {
                if (representatives.get(i).isSimilar(stack)) {
                    bucket = i;
                    break;
                }
            }
            if (bucket == -1) {
                representatives.add(stack);
                totals.add(stack.getAmount());
            } else {
                totals.set(bucket, totals.get(bucket) + stack.getAmount());
            }
        }
        List<ItemStack> merged = new ArrayList<>(representatives.size());
        for (int i = 0; i < representatives.size(); i++) {
            ItemStack sample = representatives.get(i).clone();
            sample.setAmount(totals.get(i));
            merged.add(sample);
        }
        return merged;
    }

    /** Splits each merged group into full-stack pieces and lays them into a {@code size}-slot array. */
    private static ItemStack[] layOut(List<ItemStack> groups, int size) {
        ItemStack[] rebuilt = new ItemStack[size];
        int slot = 0;
        for (ItemStack group : groups) {
            int remaining = group.getAmount();
            int maxStack = group.getMaxStackSize();
            while (remaining > 0 && slot < rebuilt.length) {
                int amount = Math.min(maxStack, remaining);
                ItemStack piece = group.clone();
                piece.setAmount(amount);
                rebuilt[slot++] = piece;
                remaining -= amount;
            }
        }
        return rebuilt;
    }
}
