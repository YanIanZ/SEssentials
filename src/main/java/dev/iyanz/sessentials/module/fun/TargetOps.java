package dev.iyanz.sessentials.module.fun;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;

/**
 * Runs an entity/world mutation for {@code target} on the correct thread: inline when
 * the command sender already <em>is</em> {@code target} (a normal command executor
 * already runs on that player's Folia region thread), otherwise hopped via
 * {@link Schedulers#entity} so entity/world API is touched safely.
 */
final class TargetOps {

    private TargetOps() {
    }

    /**
     * @param plugin the owning plugin
     * @param sender the command sender
     * @param target the player the mutation applies to
     * @param action the mutation to run
     */
    static void run(SEssentialsPlugin plugin, CommandSender sender, Player target, Runnable action) {
        if (target.equals(sender)) {
            action.run();
        } else {
            Schedulers.entity(plugin, target, action);
        }
    }
}
