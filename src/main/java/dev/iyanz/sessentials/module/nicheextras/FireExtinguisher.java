package dev.iyanz.sessentials.module.nicheextras;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Clears burning on every online player ({@code /extinguishall}). Each player's fire
 * ticks are reset on that player's own region thread (Folia-safe).
 */
final class FireExtinguisher {

    private FireExtinguisher() {
    }

    /**
     * Sets fire ticks to zero for all online players.
     *
     * @param plugin the owning plugin (for scheduling)
     * @param sender the command sender to report back to
     * @return 1 (always handled)
     */
    static int extinguishAll(SEssentialsPlugin plugin, CommandSender sender) {
        int count = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            Schedulers.entity(plugin, online, () -> online.setFireTicks(0));
            count++;
        }
        Msg.ok(sender, "Extinguished fire on " + count + (count == 1 ? " player." : " players."));
        return 1;
    }
}
