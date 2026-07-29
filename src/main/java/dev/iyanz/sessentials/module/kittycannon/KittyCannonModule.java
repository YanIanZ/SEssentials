package dev.iyanz.sessentials.module.kittycannon;

import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Player;

/**
 * KittyCannon — a harmless novelty command that launches an exploding cat.
 *
 * <p>{@code /kittycannon} (alias {@code /kc}) spawns a {@link Cat} at the sender's
 * eye location, flings it forward in the direction they are looking, and — after a
 * short fuse — removes the cat and triggers a purely cosmetic explosion at its final
 * position. The explosion uses power {@code 0} with fire and block-breaking disabled,
 * so it is pure spectacle: no terrain damage and no grief.</p>
 *
 * <p>Folia: a command executor already runs on the sender's region thread, so the
 * cat is spawned there directly. The delayed explosion is scheduled on the cat's own
 * entity scheduler ({@link org.bukkit.entity.Entity#getScheduler()}), which keeps the
 * follow-up on whatever region owns the cat when the fuse burns out — even if it has
 * flown across a region boundary.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class KittyCannonModule implements EssModule {

    /** Forward launch speed applied to the cat's velocity, in blocks/tick. */
    private static final double LAUNCH_SPEED = 1.5;

    /** Fuse length before the cat detonates, in server ticks (~2 seconds). */
    private static final long FUSE_TICKS = 40L;

    @Override
    public String name() {
        return "kittycannon";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("kittycannon")
                .requires(s -> s.getSender().hasPermission("sessentials.kittycannon"))
                .executes(Cmds.playerExec(player -> launch(plugin, player)))
                .build(), "Launch an exploding cat", List.of("kc")));
    }

    /**
     * Spawns and fires the cat for {@code player}, scheduling its cosmetic explosion.
     * Runs on the sender's region thread (the command executor already is).
     *
     * @param plugin the owning plugin, used to schedule on the cat's entity scheduler
     * @param player the launcher
     */
    private static void launch(SEssentialsPlugin plugin, Player player) {
        Cat cat = player.getWorld().spawn(player.getEyeLocation(), Cat.class);
        cat.setVelocity(player.getLocation().getDirection().multiply(LAUNCH_SPEED));

        player.getWorld().playSound(player.getEyeLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
        player.getWorld().playSound(player.getEyeLocation(), Sound.ENTITY_CAT_AMBIENT, 1.0f, 1.5f);

        cat.getScheduler().runDelayed(plugin, task -> {
            Location loc = cat.getLocation();
            cat.remove();
            loc.getWorld().createExplosion(loc, 0f, false, false);
            loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
        }, null, FUSE_TICKS);

        Msg.ok(player, "Kitty away!");
    }
}
