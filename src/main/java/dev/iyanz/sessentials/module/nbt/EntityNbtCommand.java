package dev.iyanz.sessentials.module.nbt;

import java.util.List;
import java.util.Set;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.RayTraceResult;

/**
 * Registers {@code /entitynbt} (alias {@code /entityinfo2}): a read-only report about
 * the entity currently in the sender's crosshair — type, UUID, plain custom name and,
 * for {@link LivingEntity living entities}, health and active potion effects — plus
 * any custom {@link org.bukkit.persistence.PersistentDataContainer} keys.
 *
 * <p>Only ever ray-traces the sender's own line of sight, so it runs safely on their
 * own region thread with no scheduler hop, and never mutates the target.</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class EntityNbtCommand {

    private static final int MAX_DISTANCE = 10;

    private EntityNbtCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            var node = Commands.literal("entitynbt")
                    .requires(s -> s.getSender().hasPermission("sessentials.nbt"))
                    .executes(Cmds.playerExec(EntityNbtCommand::report))
                    .build();
            reg.register(node, "Show info about the entity you're looking at", List.of("entityinfo2"));
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

        Msg.value(player, "Entity:", NbtFormat.titleCase(target.getType().name()));
        Msg.value(player, "UUID:", target.getUniqueId().toString());

        Component customName = target.customName();
        Msg.value(player, "Custom name:",
                customName != null ? PlainTextComponentSerializer.plainText().serialize(customName) : "none");

        if (target instanceof LivingEntity living) {
            Msg.value(player, "Health:", NbtFormat.decimal(living.getHealth()) + " / " + NbtFormat.decimal(living.getMaxHealth()));
            reportPotionEffects(player, living);
        }

        reportPdcKeys(player, target);
    }

    private static void reportPotionEffects(Player player, LivingEntity living) {
        List<PotionEffect> effects = List.copyOf(living.getActivePotionEffects());
        if (effects.isEmpty()) {
            Msg.value(player, "Potion effects:", "none");
            return;
        }
        Msg.info(player, effects.size() + " potion effect(s):");
        for (PotionEffect effect : effects) {
            String name = NbtFormat.titleCase(effect.getType().getKey().getKey());
            Msg.value(player, "  " + name + ":", "Lvl " + (effect.getAmplifier() + 1) + " (" + (effect.getDuration() / 20) + "s)");
        }
    }

    private static void reportPdcKeys(Player player, Entity target) {
        Set<NamespacedKey> keys = target.getPersistentDataContainer().getKeys();
        if (keys.isEmpty()) {
            Msg.value(player, "PDC keys:", "none");
            return;
        }
        Msg.info(player, keys.size() + " custom data key(s):");
        for (NamespacedKey key : keys) {
            Msg.value(player, "  -", key.toString());
        }
    }
}
