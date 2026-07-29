package dev.iyanz.sessentials.module.glow;

import java.util.Locale;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

/**
 * Manages the shared scoreboard teams that tint a player's glow outline.
 *
 * <p>A glowing entity is rendered in the colour of the {@link Team} it belongs to on
 * the viewer's scoreboard. Because unattached players default to the server's main
 * scoreboard ({@link ScoreboardManager#getMainScoreboard()}), placing a player on a
 * team there colours their outline for everyone. This helper keeps exactly one team
 * per {@link NamedTextColor}, named {@code ess_glow_<color>}, reusing it across
 * players and creating it lazily on first use.</p>
 *
 * <p>All methods operate on the main scoreboard. Scoreboard mutation is global-safe on
 * Folia, but callers still run these on the affected player's region thread so the
 * accompanying {@code setGlowing} call stays on the correct thread.</p>
 */
final class GlowTeams {

    /** Prefix shared by every glow-colour team, used to recognise and clean them up. */
    private static final String TEAM_PREFIX = "ess_glow_";

    private GlowTeams() {
    }

    /**
     * Resolves a colour name to its {@link NamedTextColor}.
     *
     * @param name a colour name such as {@code red} or {@code light_purple} (any case)
     * @return the matching colour, or {@code null} if the name is not a known colour
     */
    static NamedTextColor color(String name) {
        return NamedTextColor.NAMES.value(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Places {@code player} on the shared team for {@code color}, first removing them
     * from any other glow team so only one colour applies. The team is created on the
     * main scoreboard if it does not yet exist.
     *
     * @param player the glowing player
     * @param color  the outline colour to apply
     */
    static void apply(Player player, NamedTextColor color) {
        Scoreboard board = mainScoreboard();
        clear(player, board);

        String teamName = TEAM_PREFIX + NamedTextColor.NAMES.key(color);
        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }
        team.color(color);

        String entry = player.getName();
        if (!team.hasEntry(entry)) {
            team.addEntry(entry);
        }
    }

    /**
     * Removes {@code player} from every glow team on the main scoreboard, reverting
     * their outline to the default white.
     *
     * @param player the player to detach from glow teams
     */
    static void clear(Player player) {
        clear(player, mainScoreboard());
    }

    /** Removes {@code player}'s entry from all glow teams on {@code board}. */
    private static void clear(Player player, Scoreboard board) {
        String entry = player.getName();
        for (Team team : board.getTeams()) {
            if (team.getName().startsWith(TEAM_PREFIX) && team.hasEntry(entry)) {
                team.removeEntry(entry);
            }
        }
    }

    /** @return the server's shared main scoreboard. */
    private static Scoreboard mainScoreboard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        return manager.getMainScoreboard();
    }
}
