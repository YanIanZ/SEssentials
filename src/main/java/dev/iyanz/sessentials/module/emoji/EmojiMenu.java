package dev.iyanz.sessentials.module.emoji;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.selib.gui.ItemBuilder;
import dev.iyanz.sessentials.selib.gui.PagedMenu;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.SmallCaps;
import dev.iyanz.sessentials.util.Style;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * SELIB {@link PagedMenu} opened by {@code /emoji}: one {@link Material#PAPER} icon per configured
 * emoji, showing the glyph as its name and the {@code :code:} token in its lore.
 *
 * <p>Minecraft has no supported way to insert text into a player's chat box, so clicking an icon
 * instead sends a <em>click-to-copy</em> chat line: the {@code :code:} token is rendered with an
 * Adventure {@link ClickEvent#copyToClipboard(String) copy-to-clipboard} action, so the player can
 * click it once to place the token on their clipboard and paste it into chat. The menu is left open
 * so several tokens can be grabbed in one visit.</p>
 *
 * <p>Folia-safe via the {@link dev.iyanz.sessentials.selib.gui.Menu} base, which opens the inventory
 * on the viewer's region thread and cancels/dispatches clicks. The {@code sessentials.emoji}
 * permission that gates {@code /emoji} is enforced at command registration in {@link EmojiModule}.</p>
 */
public final class EmojiMenu extends PagedMenu {

    private static final int ROWS = 6;
    /** Soft-yellow (matches {@link Style#HINT}) for the clickable token component. */
    private static final TextColor TOKEN_COLOR = TextColor.color(0xFFD54F);

    private final EmojiMap emojiMap;

    private EmojiMenu(SEssentialsPlugin plugin, Player viewer, EmojiMap emojiMap) {
        super(plugin, viewer, MM.deserialize(Style.title("Emojis")), ROWS);
        this.emojiMap = emojiMap;
    }

    /**
     * Opens the paginated emoji browser for {@code viewer}, or sends an informational message if no
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
        new EmojiMenu(plugin, viewer, emojiMap).open();
    }

    @Override
    protected List<PageEntry> entries() {
        List<PageEntry> out = new ArrayList<>();
        for (Map.Entry<String, String> entry : emojiMap.entries().entrySet()) {
            String code = entry.getKey();
            String emoji = entry.getValue();
            out.add(new PageEntry(icon(code, emoji), event -> copy(code, emoji)));
        }
        return out;
    }

    /** Builds the paper icon for one emoji: the glyph as the name, the {@code :code:} token in lore. */
    private static ItemStack icon(String code, String emoji) {
        String token = ":" + code + ":";
        return new ItemBuilder(Material.PAPER)
                .name(Style.VALUE + emoji)
                .lore(
                        Style.HINT + token,
                        Style.hint("Click to copy the token"))
                .clean()
                .build();
    }

    /**
     * Sends the viewer a chat line whose {@code :code:} token is click-to-copy: clicking it copies
     * the token to their clipboard so they can paste it into chat. The menu is left open.
     */
    private void copy(String code, String emoji) {
        String token = ":" + code + ":";
        Component clickable = Component.text(token, TOKEN_COLOR)
                .clickEvent(ClickEvent.copyToClipboard(token))
                .hoverEvent(HoverEvent.showText(Msg.mm(Style.GRAY + SmallCaps.of("Click to copy to clipboard"))));
        Component line = Msg.mm(Msg.prefix() + Style.GRAY + SmallCaps.of("Emoji") + " "
                        + Style.VALUE + emoji + " ")
                .append(clickable);
        viewer.sendMessage(line);
    }
}
