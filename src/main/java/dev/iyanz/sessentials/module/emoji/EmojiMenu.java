package dev.iyanz.sessentials.module.emoji;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.gui.ItemBuilder;
import dev.iyanz.sessentials.gui.Menu;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.SmallCaps;
import dev.iyanz.sessentials.util.Style;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Chest GUI opened by {@code /emoji}: one {@link Material#PAPER} icon per configured
 * emoji, showing the glyph next to its {@code :code:} and the code again in the lore.
 *
 * <p>Minecraft has no supported way to insert text into a player's chat box without
 * packets, so clicking an icon instead sends the {@code :code:} to the player as a chat
 * line they can read off (and copy) before typing it. The menu is left open so several
 * codes can be grabbed in one visit.</p>
 *
 * <p>Folia-safe via the {@link Menu} base, which opens the inventory on the viewer's
 * region thread and cancels/dispatches clicks.</p>
 */
public final class EmojiMenu extends Menu {

    private static final int COLUMNS = 9;
    private static final int MAX_ROWS = 6;

    private final List<Map.Entry<String, String>> entries;

    private EmojiMenu(SEssentialsPlugin plugin, Player viewer, List<Map.Entry<String, String>> entries) {
        super(plugin, viewer, MM.deserialize(Style.title("Emojis")), rows(entries.size()));
        this.entries = entries;
    }

    /**
     * Opens the emoji browser for {@code viewer}, or sends an informational message if no
     * emoji are configured.
     *
     * @param plugin   the owning plugin
     * @param emojiMap the shared code-to-emoji mapping
     * @param viewer   the player to show the menu to
     */
    public static void open(SEssentialsPlugin plugin, EmojiMap emojiMap, Player viewer) {
        if (emojiMap.isEmpty()) {
            Msg.info(viewer, "There are no emojis configured.");
            return;
        }
        new EmojiMenu(plugin, viewer, new ArrayList<>(emojiMap.entries().entrySet())).open();
    }

    @Override
    protected void build() {
        int size = Math.min(entries.size(), MAX_ROWS * COLUMNS);
        for (int slot = 0; slot < size; slot++) {
            Map.Entry<String, String> entry = entries.get(slot);
            set(slot, icon(entry.getKey(), entry.getValue()), event -> copy(entry.getKey()));
        }
    }

    /** Builds the paper icon for one emoji: the glyph beside its {@code :code:}, with usage lore. */
    private static ItemStack icon(String code, String emoji) {
        String token = ":" + code + ":";
        return new ItemBuilder(Material.PAPER)
                .name(Style.VALUE + emoji + " " + Style.HINT + token)
                .lore(
                        Style.GRAY + SmallCaps.of("Type ") + Style.HINT + token
                                + Style.GRAY + SmallCaps.of(" in chat to use"),
                        Style.hint("Click to copy the code"))
                .clean()
                .build();
    }

    /** Sends {@code :code:} to the viewer so they can read and copy it, keeping the menu open. */
    private void copy(String code) {
        Msg.value(viewer, "Emoji code:", ":" + code + ":");
    }

    /** @return a row count (1-{@value #MAX_ROWS}) sized to fit {@code count} icons. */
    private static int rows(int count) {
        return Math.max(1, Math.min(MAX_ROWS, (count + COLUMNS - 1) / COLUMNS));
    }
}
