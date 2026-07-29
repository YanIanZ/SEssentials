package dev.iyanz.sessentials.module.fireball;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.util.Vector;

/**
 * Fireball module: {@code /fireball} launches a fireball from the sender's eyes in
 * the direction they are looking.
 *
 * <p>Behaviour:</p>
 * <ul>
 *   <li>{@code /fireball} — launches a small (blaze-style) fireball.</li>
 *   <li>{@code /fireball large} — launches a large (ghast-style) fireball.</li>
 * </ul>
 *
 * <p>Both forms require {@code sessentials.fireball}. The projectile is spawned via
 * {@link Player#launchProjectile(Class, Vector)}, which attributes the sender as the
 * shooter; on Folia the command executor already runs on the sender's region thread,
 * so spawning next to the sender needs no scheduler hop.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class FireballModule implements EssModule {

    /** Multiplier applied to the sender's look direction as the launch velocity. */
    private static final double SPEED = 2.0D;

    @Override
    public String name() {
        return "fireball";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("fireball")
                .requires(s -> s.getSender().hasPermission("sessentials.fireball"))
                .executes(Cmds.playerExec(player -> launch(player, SmallFireball.class)))
                .then(Commands.literal("large")
                        .executes(Cmds.playerExec(player -> launch(player, LargeFireball.class))))
                .build(), "Launch a fireball where you are looking"));
    }

    /**
     * Launches a fireball of the given type along the player's line of sight.
     *
     * @param player the shooter (already on its own region thread)
     * @param type   the concrete fireball entity type to spawn
     */
    private static void launch(Player player, Class<? extends Fireball> type) {
        Vector velocity = player.getEyeLocation().getDirection().multiply(SPEED);
        player.launchProjectile(type, velocity);
        Msg.ok(player, "Whoosh!");
    }
}
