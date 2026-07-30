package dev.iyanz.sessentials.module.serverextras;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Miscellaneous server-administration extras:
 * <ul>
 *   <li>{@code /giveall <item> [amount]} — hand an item to every online player.</li>
 *   <li>{@code /sound <sound> [player] [volume] [pitch]} — play a sound to a single
 *       player (the sender by default), unlike the broadcast {@code /bsound}.</li>
 *   <li>{@code /viewdistance <n> [player]} — set a player's per-player send view
 *       distance (Paper API).</li>
 *   <li>{@code /maxplayers <n>} — change the server's player slot count live.</li>
 *   <li>{@code /playtimetop} — ranked leaderboard of total play time, read from the
 *       same {@link org.bukkit.Statistic#PLAY_ONE_MINUTE} statistic the
 *       {@code playtime} module reports (Bukkit persists it per player).</li>
 * </ul>
 *
 * <p>All commands are operator-oriented ({@code sessentials.<command>} permissions)
 * and Folia-safe: per-player work hops onto the owning player's region thread and
 * server-global work runs on the global region scheduler.</p>
 */
public final class ServerExtrasModule implements EssModule {

    @Override
    public String name() {
        return "serverextras";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        GiveAllCommand.register(plugin);
        SoundCommand.register(plugin);
        ViewDistanceCommand.register(plugin);
        MaxPlayersCommand.register(plugin);
        PlaytimeTopCommand.register(plugin);
    }
}
