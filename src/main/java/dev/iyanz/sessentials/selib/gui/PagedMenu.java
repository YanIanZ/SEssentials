package dev.iyanz.sessentials.selib.gui;

import java.util.List;
import java.util.function.Consumer;

import dev.iyanz.sessentials.SEssentialsPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * SELIB base for menus that render an arbitrary {@link List} of {@link PageEntry entries}
 * across pages. Entries fill the content region — every row except the bottom navigation
 * row — and the bottom row carries a previous button (first slot), a page indicator (middle)
 * and a next button (last slot). Paging re-renders in place: the backing inventory is rebuilt
 * and its contents replaced without reopening the view.
 *
 * <p>Subclasses supply the data via {@link #entries()} and are free to return a different list
 * each build (e.g. after a filter changes); {@link #refresh()} re-renders from the current page.</p>
 */
public abstract class PagedMenu extends Menu {

    /**
     * A single pageable entry: the {@code icon} shown in a content slot and an optional
     * {@code onClick} handler invoked when it is clicked.
     */
    public record PageEntry(ItemStack icon, Consumer<InventoryClickEvent> onClick) {
    }

    /** First slot of the bottom navigation row ({@code rows*9 - 9}). */
    private final int navRowStart;
    /** Number of content slots per page ({@code (rows-1)*9}). */
    private final int pageSize;
    private int page;

    protected PagedMenu(SEssentialsPlugin plugin, Player viewer, Component title, int rows) {
        super(plugin, viewer, title, rows);
        this.navRowStart = getInventory().getSize() - 9;
        this.pageSize = Math.max(1, navRowStart);
    }

    /** @return the entries to paginate; may be recomputed on each call. */
    protected abstract List<PageEntry> entries();

    @Override
    protected void build() {
        clear();

        final List<PageEntry> all = entries();
        final int totalPages = Math.max(1, (int) Math.ceil(all.size() / (double) pageSize));
        page = Math.max(0, Math.min(page, totalPages - 1));

        // Content region: the current page's slice of entries.
        final int start = page * pageSize;
        for (int i = 0; i < pageSize && start + i < all.size(); i++) {
            final PageEntry entry = all.get(start + i);
            set(i, entry.icon(), entry.onClick());
        }

        // Navigation row.
        for (int slot = navRowStart; slot < getInventory().getSize(); slot++) {
            set(slot, Buttons.filler(), null);
        }
        if (page > 0) {
            set(navRowStart, Buttons.prev(page), event -> {
                page--;
                refresh();
            });
        }
        set(navRowStart + 4, Buttons.pageIndicator(page + 1, totalPages), null);
        if (page < totalPages - 1) {
            set(navRowStart + 8, Buttons.next(page + 2), event -> {
                page++;
                refresh();
            });
        }
    }

    /**
     * Re-renders the current page in place. Because the backing inventory is the live view,
     * rebuilding it updates the open menu without a reopen. Must run on the viewer's region
     * thread (paging handlers already do).
     */
    protected void refresh() {
        build();
        viewer.updateInventory();
    }
}
