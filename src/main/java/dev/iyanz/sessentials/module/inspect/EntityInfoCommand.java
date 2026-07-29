package dev.iyanz.sessentials.module.inspect;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

/**
 * Registers {@code /entityinfo}: reports the type, UUID, location, custom name and
 * (for living entities) health of the entity the sender is currently looking at,
 * within {@value #MAX_DISTANCE} blocks. Player-only, and only ever ray-traces the
 * sender's own line of sight, so it runs safely on their own region thread without
 * any scheduler hop.
 */
@SuppressWarnings({"UnstableApiUsage", "deprecation"})
final class EntityInfoCommand {

    private static final int MAX_DISTANCE = 10;

    private EntityInfoCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            var node = Commands.literal("entityinfo")
                    .requires(s -> s.getSender().hasPermission("sessentials.entityinfo"))
                    .executes(Cmds.playerExec(EntityInfoCommand::report))
                    .build();
            reg.register(node, "Show info about the entity you're looking at");
        });
    }

    private static void report(Player player) {
        // false: stop the ray at the first solid block, matching what's actually in the crosshair.
        RayTraceResult trace = player.rayTraceEntities(MAX_DISTANCE, false);
        Entity target = trace == null ? null : trace.getHitEntity();
        if (target == null) {
            Msg.err(player, "No entity in sight within " + MAX_DISTANCE + " blocks.");
            return;
        }

        Msg.value(player, "Entity:", InspectFormat.titleCase(target.getType().name()));
        Msg.value(player, "UUID:", target.getUniqueId().toString());
        Msg.value(player, "Location:", InspectFormat.location(target.getLocation()));
        Component customName = target.customName();
        Msg.value(player, "Custom name:",
                customName != null ? PlainTextComponentSerializer.plainText().serialize(customName) : "none");
        if (target instanceof LivingEntity living) {
            Msg.value(player, "Health:", InspectFormat.decimal(living.getHealth()) + " / " + InspectFormat.decimal(living.getMaxHealth()));
        }
    }
}
