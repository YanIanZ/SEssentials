package dev.iyanz.sessentials.module.menus;

import java.util.ArrayList;
import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.selib.gui.ItemBuilder;
import dev.iyanz.sessentials.selib.gui.PagedMenu;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.SmallCaps;
import dev.iyanz.sessentials.util.Style;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * SELIB paginated help browser: one icon per top-level command category. Clicking a category
 * sends the viewer a short, readable list of that category's commands in chat (via {@link Msg}),
 * leaving the menu open so several categories can be browsed in one sitting.
 *
 * <p>This is purely informational — the listed command literals are shown to everyone regardless
 * of individual command permissions; the only gate is {@code sessentials.helpmenu} on the
 * {@code /helpmenu} command that opens it. Command literals in the chat output are kept in plain
 * ASCII (only the descriptions are small-capped) so players can read and copy them verbatim.</p>
 *
 * <p>Opened by {@code /helpmenu}; see {@link MenusModule}. Folia-safe via the {@link PagedMenu}
 * base, which opens on the viewer's region thread and dispatches clicks there.</p>
 */
public final class HelpMenu extends PagedMenu {

    /** Soft-yellow hint colour (matches {@link Style#HINT}) for command literals in chat/lore. */
    private static final TextColor HINT_COLOR = TextColor.color(0xFFD54F);
    /** Muted grey colour (matches {@link Style#GRAY}) for command descriptions in chat. */
    private static final TextColor GRAY_COLOR = TextColor.color(0x9AA0A6);

    /** A single command line within a category: the {@code usage} literal and a short {@code description}. */
    private record Line(String usage, String description) {
    }

    /** A browsable command category: an {@code icon}, a {@code name} and its command {@link Line}s. */
    private record Category(String name, Material icon, List<Line> lines) {
    }

    private static final List<Category> CATEGORIES = List.of(
            new Category("Teleport", Material.ENDER_PEARL, List.of(
                    new Line("/tpa <player>", "request to teleport to a player"),
                    new Line("/tpahere <player>", "ask a player to teleport to you"),
                    new Line("/tpaccept", "accept a pending request"),
                    new Line("/tpdeny", "deny a pending request"),
                    new Line("/back", "return to your last location"),
                    new Line("/spawn", "teleport to spawn"),
                    new Line("/rtp", "teleport to a random location"),
                    new Line("/top", "teleport to the highest block"))),
            new Category("Homes & Warps", Material.RED_BED, List.of(
                    new Line("/sethome [name]", "set a home"),
                    new Line("/home [name]", "teleport to a home"),
                    new Line("/homes", "list your homes"),
                    new Line("/delhome <name>", "delete a home"),
                    new Line("/warp <name>", "teleport to a warp"),
                    new Line("/warps", "browse the warps menu"))),
            new Category("Economy", Material.GOLD_INGOT, List.of(
                    new Line("/balance", "check your balance"),
                    new Line("/pay <player> <amount>", "send money to a player"),
                    new Line("/baltop", "view the richest players"),
                    new Line("/sell", "sell the item in your hand"),
                    new Line("/worth", "check an item's value"),
                    new Line("/lottery", "buy a lottery ticket"))),
            new Category("Fun", Material.FIREWORK_ROCKET, List.of(
                    new Line("/coinflip", "flip a coin"),
                    new Line("/kittycannon", "launch an explosive kitten"),
                    new Line("/firework", "launch a firework"),
                    new Line("/lightning", "strike lightning where you look"),
                    new Line("/me <action>", "broadcast an action message"))),
            new Category("Player", Material.LEATHER_CHESTPLATE, List.of(
                    new Line("/heal", "restore your health"),
                    new Line("/feed", "restore your hunger"),
                    new Line("/fly", "toggle flight"),
                    new Line("/speed <amount>", "set your movement speed"),
                    new Line("/hat", "wear the held item as a hat"),
                    new Line("/nick <name>", "change your nickname"))),
            new Category("Chat & Social", Material.WRITABLE_BOOK, List.of(
                    new Line("/msg <player> <text>", "send a private message"),
                    new Line("/reply <text>", "reply to the last message"),
                    new Line("/mail", "read and send mail"),
                    new Line("/ignore <player>", "ignore a player"),
                    new Line("/helpop <text>", "message online staff"),
                    new Line("/report <player>", "report a player"))),
            new Category("World & Time", Material.CLOCK, List.of(
                    new Line("/time <value>", "set the world time"),
                    new Line("/weather <type>", "set the world weather"),
                    new Line("/ptime <value>", "set your personal time"),
                    new Line("/pweather <type>", "set your personal weather"),
                    new Line("/ptimemenu", "open the time & weather menu"),
                    new Line("/world <name>", "teleport between worlds"))),
            new Category("Kits & Items", Material.CHEST, List.of(
                    new Line("/kit <name>", "claim a kit"),
                    new Line("/kits", "browse the kits menu"),
                    new Line("/repair", "repair the held item"),
                    new Line("/enchant <name> <lvl>", "enchant the held item"),
                    new Line("/more", "fill the held stack"),
                    new Line("/backpack", "open your backpack"))),
            new Category("Moderation", Material.SHIELD, List.of(
                    new Line("/ban <player>", "ban a player"),
                    new Line("/kick <player>", "kick a player"),
                    new Line("/mute <player>", "mute a player"),
                    new Line("/warn <player>", "warn a player"),
                    new Line("/jail <player>", "jail a player"),
                    new Line("/vanish", "toggle invisibility"))));

    private HelpMenu(SEssentialsPlugin plugin, Player viewer) {
        super(plugin, viewer, MM.deserialize(Style.title("Help")), 6);
    }

    /**
     * Builds and opens the help menu for {@code viewer}.
     *
     * @param plugin the owning plugin
     * @param viewer the player to show the menu to
     */
    public static void open(SEssentialsPlugin plugin, Player viewer) {
        new HelpMenu(plugin, viewer).open();
    }

    @Override
    protected List<PageEntry> entries() {
        List<PageEntry> out = new ArrayList<>(CATEGORIES.size());
        for (Category category : CATEGORIES) {
            out.add(new PageEntry(icon(category), event -> send(category)));
        }
        return out;
    }

    /**
     * Builds a category icon whose lore previews its command count and the bare literals of a
     * few commands. Only the base literal (up to the first space) is previewed, so no
     * {@code <arg>} placeholders reach MiniMessage parsing.
     */
    private ItemStack icon(Category category) {
        List<String> lore = new ArrayList<>();
        lore.add(Style.lore(category.lines().size() + " commands"));
        int preview = Math.min(3, category.lines().size());
        for (int i = 0; i < preview; i++) {
            lore.add(Style.HINT + base(category.lines().get(i).usage()));
        }
        lore.add(Style.hint("Click to list them in chat"));
        return new ItemBuilder(category.icon)
                .name(Style.button(Style.INFO, category.name()))
                .lore(lore)
                .clean()
                .build();
    }

    /**
     * Sends the full command list for {@code category} to the viewer as chat, keeping the menu
     * open. Each line is built as a literal {@link Component} (not MiniMessage), so the
     * {@code <arg>} placeholders in the usage strings are shown verbatim and never parsed as tags.
     */
    private void send(Category category) {
        Msg.raw(viewer, Style.title(category.name()));
        for (Line line : category.lines()) {
            Component chatLine = Component.text(line.usage() + " ", HINT_COLOR)
                    .append(Component.text(SmallCaps.of("- " + line.description()), GRAY_COLOR));
            viewer.sendMessage(chatLine);
        }
    }

    /** @return the base literal of {@code usage} (up to the first space), e.g. {@code "/tpa"}. */
    private static String base(String usage) {
        int space = usage.indexOf(' ');
        return space < 0 ? usage : usage.substring(0, space);
    }
}
