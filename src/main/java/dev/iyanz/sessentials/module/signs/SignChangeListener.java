package dev.iyanz.sessentials.module.signs;

import java.util.Locale;
import java.util.Optional;

import dev.iyanz.sessentials.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

/**
 * Handles a sign edit ({@link SignChangeEvent}):
 * <ol>
 *   <li>If the first line matches a recognised {@link SignTag}, the editor must hold
 *       {@code sessentials.signs.create} or the edit is denied (event cancelled).</li>
 *   <li>If the editor holds {@code sessentials.sign.color}, every non-empty line is
 *       re-parsed through MiniMessage (legacy {@code &} codes accepted via
 *       {@link dev.iyanz.sessentials.util.LegacyColors}) and set back as a rich
 *       component.</li>
 * </ol>
 */
final class SignChangeListener implements Listener {

    private static final String CREATE_PERMISSION = "sessentials.signs.create";
    private static final String COLOR_PERMISSION = "sessentials.sign.color";
    private static final int LINE_COUNT = 4;

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        String firstLine = normalize(event.line(0));
        Optional<SignTag> tag = SignTag.match(firstLine);

        if (tag.isPresent() && !player.hasPermission(CREATE_PERMISSION)) {
            event.setCancelled(true);
            Msg.err(player, "You don't have permission to create action signs.");
            return;
        }

        if (!player.hasPermission(COLOR_PERMISSION)) {
            return;
        }
        for (int i = 0; i < LINE_COUNT; i++) {
            Component line = event.line(i);
            if (line == null) {
                continue;
            }
            String raw = PlainTextComponentSerializer.plainText().serialize(line);
            if (raw.isEmpty()) {
                continue;
            }
            event.line(i, Msg.mm(raw));
        }
    }

    /**
     * Strips any colour formatting from a sign line and returns trimmed, lower-case
     * plain text suitable for {@link SignTag#match(String)}. Legacy {@code &}/{@code §}
     * codes typed by the editor (not yet parsed at edit time) are converted and then
     * re-serialized away by round-tripping the line through {@link Msg#mm}.
     */
    private static String normalize(Component line) {
        if (line == null) {
            return "";
        }
        String raw = PlainTextComponentSerializer.plainText().serialize(line);
        String stripped = PlainTextComponentSerializer.plainText().serialize(Msg.mm(raw));
        return stripped.trim().toLowerCase(Locale.ROOT);
    }
}
