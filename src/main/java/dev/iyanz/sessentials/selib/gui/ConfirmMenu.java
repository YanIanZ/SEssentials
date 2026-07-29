package dev.iyanz.sessentials.selib.gui;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.util.Style;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * SELIB three-row yes/no confirmation dialog. A green confirm button sits on the right, a red
 * cancel button on the left, and the {@code prompt} item in the centre; the remainder is
 * filled with panes. Confirming runs {@code onConfirm} then closes the menu; cancelling runs
 * the (nullable) {@code onCancel} then closes.
 */
public final class ConfirmMenu extends Menu {

    private static final int SLOT_CANCEL = 11;
    private static final int SLOT_PROMPT = 13;
    private static final int SLOT_CONFIRM = 15;

    private final ItemStack prompt;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    public ConfirmMenu(SEssentialsPlugin plugin, Player viewer, Component title, ItemStack prompt,
                       Runnable onConfirm, Runnable onCancel) {
        super(plugin, viewer, title, 3);
        this.prompt = prompt;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    protected void build() {
        fill(Buttons.filler());

        set(SLOT_PROMPT, prompt, null);

        set(SLOT_CONFIRM, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                .name(Style.button(Style.DEPOSIT, "Confirm"))
                .lore(Style.hint("Click to confirm"))
                .clean()
                .build(), event -> {
            if (onConfirm != null) {
                onConfirm.run();
            }
            viewer.closeInventory();
        });

        set(SLOT_CANCEL, new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .name(Style.button(Style.DANGER, "Cancel"))
                .lore(Style.hint("Click to cancel"))
                .clean()
                .build(), event -> {
            if (onCancel != null) {
                onCancel.run();
            }
            viewer.closeInventory();
        });
    }
}
