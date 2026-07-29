package dev.iyanz.sessentials.module.joinmessages;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Rewrites the join and quit broadcasts on every {@link PlayerJoinEvent} and
 * {@link PlayerQuitEvent} per the current {@code join-messages.*} config.
 *
 * <p>Both events fire on the involved player's own region thread, and setting the
 * message is pure event data with no entity/world mutation, so no scheduler hop is
 * needed here. All config values are read fresh on each event so a
 * {@code /joinmessages reload} takes effect immediately.</p>
 */
final class JoinMessagesListener implements Listener {

    /** Default line for a returning player joining. */
    private static final String DEFAULT_JOIN = "<yellow>%player% <gray>joined.";

    /** Default line for a player leaving. */
    private static final String DEFAULT_QUIT = "<yellow>%player% <gray>left.";

    /** Default line for a player's very first join. */
    private static final String DEFAULT_FIRST_JOIN = "<gold>Welcome <yellow>%player%<gold>!";

    private final SEssentialsPlugin plugin;

    /**
     * @param plugin the owning plugin, used for its live config values
     */
    JoinMessagesListener(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Sets the join broadcast, using the first-join line for a player who has never played before. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled()) {
            return;
        }
        if (silent()) {
            event.joinMessage(null);
            return;
        }
        Player player = event.getPlayer();
        String template = player.hasPlayedBefore()
                ? plugin.getConfig().getString("join-messages.join", DEFAULT_JOIN)
                : plugin.getConfig().getString("join-messages.first-join", DEFAULT_FIRST_JOIN);
        event.joinMessage(render(template, player));
    }

    /** Sets the quit broadcast per the current config. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!enabled()) {
            return;
        }
        if (silent()) {
            event.quitMessage(null);
            return;
        }
        Player player = event.getPlayer();
        String template = plugin.getConfig().getString("join-messages.quit", DEFAULT_QUIT);
        event.quitMessage(render(template, player));
    }

    /** @return whether the module is enabled ({@code join-messages.enabled}, default {@code false}). */
    private boolean enabled() {
        return plugin.getConfig().getBoolean("join-messages.enabled", false);
    }

    /** @return whether messages are suppressed entirely ({@code join-messages.silent}, default {@code false}). */
    private boolean silent() {
        return plugin.getConfig().getBoolean("join-messages.silent", false);
    }

    /**
     * Substitutes {@code %player%} with the player's name and parses the result as MiniMessage.
     *
     * @param template the raw config line (may contain {@code %player%} and MiniMessage tags)
     * @param player   the player the message is about
     * @return the rendered component
     */
    private static Component render(String template, Player player) {
        String line = template == null ? "" : template.replace("%player%", player.getName());
        return Msg.mm(line);
    }
}
