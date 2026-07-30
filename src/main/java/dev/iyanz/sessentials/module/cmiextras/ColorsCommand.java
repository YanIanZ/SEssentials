package dev.iyanz.sessentials.module.cmiextras;

import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

/**
 * {@code /colors} — sends a rendered colour-code reference: every legacy {@code &}
 * colour shown in its own colour, the formatting codes with their decoration applied,
 * and the common MiniMessage forms with live samples. Lines are built directly as
 * Adventure components so the literal codes are displayed verbatim instead of being
 * parsed away. Requires {@code sessentials.colors}.
 */
@SuppressWarnings("UnstableApiUsage")
final class ColorsCommand {

    /** One legacy colour swatch: its {@code &}-code, display name and colour. */
    private record Swatch(char code, String label, TextColor color) {
    }

    /** The sixteen legacy colours in code order, rendered four per line. */
    private static final Swatch[] COLORS = {
            new Swatch('0', "Black", NamedTextColor.BLACK),
            new Swatch('1', "Dark Blue", NamedTextColor.DARK_BLUE),
            new Swatch('2', "Dark Green", NamedTextColor.DARK_GREEN),
            new Swatch('3', "Dark Aqua", NamedTextColor.DARK_AQUA),
            new Swatch('4', "Dark Red", NamedTextColor.DARK_RED),
            new Swatch('5', "Purple", NamedTextColor.DARK_PURPLE),
            new Swatch('6', "Gold", NamedTextColor.GOLD),
            new Swatch('7', "Gray", NamedTextColor.GRAY),
            new Swatch('8', "Dark Gray", NamedTextColor.DARK_GRAY),
            new Swatch('9', "Blue", NamedTextColor.BLUE),
            new Swatch('a', "Green", NamedTextColor.GREEN),
            new Swatch('b', "Aqua", NamedTextColor.AQUA),
            new Swatch('c', "Red", NamedTextColor.RED),
            new Swatch('d', "Pink", NamedTextColor.LIGHT_PURPLE),
            new Swatch('e', "Yellow", NamedTextColor.YELLOW),
            new Swatch('f', "White", NamedTextColor.WHITE),
    };

    /** Number of colour swatches shown per chat line. */
    private static final int PER_LINE = 4;

    private ColorsCommand() {
    }

    /**
     * Registers {@code /colors}.
     *
     * @param reg the Paper command registrar
     */
    static void register(Commands reg) {
        reg.register(Commands.literal("colors")
                .requires(s -> s.getSender().hasPermission("sessentials.colors"))
                .executes(ctx -> show(ctx.getSource().getSender()))
                .build(), "Show the colour-code and MiniMessage reference");
    }

    /** Sends the full palette reference to {@code sender}. */
    private static int show(CommandSender sender) {
        Msg.info(sender, "Colour codes:");
        for (int start = 0; start < COLORS.length; start += PER_LINE) {
            Component line = Component.empty();
            for (int i = start; i < Math.min(start + PER_LINE, COLORS.length); i++) {
                Swatch s = COLORS[i];
                line = line.append(Component.text("&" + s.code() + " " + s.label() + "  ", s.color()));
            }
            sender.sendMessage(line);
        }

        Msg.info(sender, "Formatting codes:");
        sender.sendMessage(Component.empty()
                .append(Component.text("&l Bold  ", NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text("&o Italic  ", NamedTextColor.WHITE, TextDecoration.ITALIC))
                .append(Component.text("&n Underline  ", NamedTextColor.WHITE, TextDecoration.UNDERLINED))
                .append(Component.text("&m Strike  ", NamedTextColor.WHITE, TextDecoration.STRIKETHROUGH))
                .append(Component.text("&k ", NamedTextColor.WHITE))
                .append(Component.text("Magic", NamedTextColor.WHITE, TextDecoration.OBFUSCATED))
                .append(Component.text("  &r Reset", NamedTextColor.WHITE)));

        Msg.info(sender, "MiniMessage:");
        sender.sendMessage(Component.empty()
                .append(Component.text("<gold>text</gold>  ", NamedTextColor.GOLD))
                .append(Component.text("<#FF9900>hex</#FF9900>  ", TextColor.color(0xFF9900)))
                .append(Component.text("<gradient:#43C6AC:#4FC3F7>", NamedTextColor.GRAY))
                .append(Msg.mm("<gradient:#43C6AC:#4FC3F7>gradient")));
        return 1;
    }
}
