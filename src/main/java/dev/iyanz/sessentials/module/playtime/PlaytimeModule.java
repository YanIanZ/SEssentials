package dev.iyanz.sessentials.module.playtime;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Session-history commands: {@code /playtime} (alias {@code /pt}) reports total time
 * played, and {@code /firstjoined} (alias {@code /firstseen}) reports when a player
 * first connected. Neither command needs its own persistence — both ride on data
 * Bukkit/Paper already tracks ({@link org.bukkit.Statistic#PLAY_ONE_MINUTE} and
 * {@link org.bukkit.OfflinePlayer#getFirstPlayed()}), keeping this module a thin,
 * read-only reporting layer.
 */
public final class PlaytimeModule implements EssModule {

    @Override
    public String name() {
        return "playtime";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        PlaytimeCommand.register(plugin);
        FirstJoinedCommand.register(plugin);
    }
}
