package dev.iyanz.sessentials.module.painting;

import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Art;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

/**
 * Registers {@code /painting} (alias {@code /paintings}): cycles the {@link Art} of the
 * painting the sender is currently looking at to the next value, wrapping around after
 * the last one.
 *
 * <p>The sender's line of sight is ray-traced on their own region thread (safe, since a
 * normal command executor already runs there), but the painting itself is only ever
 * mutated on its own region thread via {@link Schedulers#entity}, since it may be owned
 * by a different Folia region than the sender.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class PaintingModule implements EssModule {

    private static final String PERMISSION = "sessentials.painting";
    private static final double MAX_DISTANCE = 6.0;

    @Override
    public String name() {
        return "painting";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(
                Commands.literal("painting")
                        .requires(s -> s.getSender().hasPermission(PERMISSION))
                        .executes(Cmds.playerExec(p -> cycleArt(plugin, p)))
                        .build(),
                "Cycle the art of the painting you're looking at",
                List.of("paintings")));
    }

    /**
     * Ray-traces {@code player}'s line of sight for a {@link Painting} and, if found,
     * advances its art to the next value in {@link Art#values()} (wrapping around) on
     * the painting's own region thread.
     *
     * @param plugin the owning plugin, used to reach the painting's scheduler
     * @param player the sender whose crosshair is checked
     */
    private static void cycleArt(SEssentialsPlugin plugin, Player player) {
        RayTraceResult trace = player.rayTraceEntities((int) MAX_DISTANCE, false);
        Entity hit = trace == null ? null : trace.getHitEntity();
        if (!(hit instanceof Painting painting)) {
            Msg.err(player, "No painting in sight within " + (int) MAX_DISTANCE + " blocks.");
            return;
        }
        Schedulers.entity(plugin, painting, () -> {
            Art[] arts = Art.values();
            int currentIndex = indexOf(arts, painting.getArt());
            Art next = arts[(currentIndex + 1) % arts.length];
            if (painting.setArt(next, true)) {
                Msg.ok(player, "Painting set to " + displayName(next) + ".");
            } else {
                Msg.err(player, "Not enough room here for " + displayName(next) + ".");
            }
        });
    }

    /** @return the index of {@code value} in {@code arts}, or {@code -1} if not found. */
    private static int indexOf(Art[] arts, Art value) {
        for (int i = 0; i < arts.length; i++) {
            if (arts[i].equals(value)) {
                return i;
            }
        }
        return -1;
    }

    /** @return {@code art}'s registry key in title case, e.g. {@code "Skull And Roses"}. */
    private static String displayName(Art art) {
        String[] parts = art.key().value().split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }
}
