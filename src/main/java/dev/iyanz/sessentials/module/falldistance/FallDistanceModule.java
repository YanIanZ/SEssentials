package dev.iyanz.sessentials.module.falldistance;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

/**
 * Registers {@code /falldistance}: reports the sender's current fall distance (in
 * blocks), and {@code /falldistance reset} zeroes it out so the player's current
 * fall deals no damage on landing.
 *
 * <p>Both the base command and {@code reset} act on the command sender only, so a
 * normal player command executor is already running on that player's own region
 * thread (Folia-safe) — no scheduler hop is required.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class FallDistanceModule implements EssModule {

    private static final String PERMISSION = "sessentials.falldistance";

    @Override
    public String name() {
        return "falldistance";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(
                Commands.literal("falldistance")
                        .requires(s -> s.getSender().hasPermission(PERMISSION))
                        .executes(Cmds.playerExec(FallDistanceModule::report))
                        .then(Commands.literal("reset")
                                .executes(Cmds.playerExec(FallDistanceModule::reset)))
                        .build(),
                "Show or reset your fall distance"));
    }

    /** Reports the sender's current fall distance. */
    private static void report(Player player) {
        Msg.value(player, "Fall distance:", String.format("%.2f", player.getFallDistance()));
    }

    /** Zeroes the sender's fall distance, preventing damage from the current fall. */
    private static void reset(Player player) {
        player.setFallDistance(0f);
        Msg.ok(player, "Your fall distance was reset.");
    }
}
