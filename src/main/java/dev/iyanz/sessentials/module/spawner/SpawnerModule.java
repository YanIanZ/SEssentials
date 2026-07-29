package dev.iyanz.sessentials.module.spawner;

import java.util.Locale;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

/**
 * Registers {@code /spawner}: inspects or changes the mob type of the spawner block the
 * sender is looking at, within {@value #REACH} blocks.
 *
 * <ul>
 *   <li>{@code /spawner} — reports the looked-at spawner's current mob type.</li>
 *   <li>{@code /spawner <entityType>} — sets the spawner's mob type to a spawnable
 *       entity type (tab-completed).</li>
 * </ul>
 *
 * <p>Folia-safe: the sender's line of sight is ray-cast from their own region thread
 * (already safe inside a command executor), but the spawner's {@link BlockState} is only
 * ever read or written on <em>the block's own region thread</em> via Paper's
 * {@link Bukkit#getRegionScheduler()}, re-fetching the state there. Result messages are
 * delivered back on the player's region thread via {@link Schedulers#entity}, since a
 * targeted block may sit in a different region than the player.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class SpawnerModule implements EssModule {

    /** How far (in blocks) the sender may be from the spawner they target. */
    private static final int REACH = 6;

    private SEssentialsPlugin plugin;

    @Override
    public String name() {
        return "spawner";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.plugin = plugin;
        plugin.commands(reg -> reg.register(Commands.literal("spawner")
                .requires(s -> s.getSender().hasPermission("sessentials.spawner"))
                .executes(Cmds.playerExec(this::report))
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests(SpawnerTypes.SUGGESTIONS)
                        .executes(this::setType))
                .build(), "Inspect or set the mob type of the spawner you're looking at"));
    }

    // --- /spawner (no argument): report the current type -------------------------

    private void report(Player player) {
        Block target = player.getTargetBlockExact(REACH);
        if (target == null) {
            Msg.err(player, "You must be looking at a spawner.");
            return;
        }
        Location loc = target.getLocation();
        Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
            CreatureSpawner spawner = spawnerAt(loc);
            if (spawner == null) {
                Schedulers.entity(plugin, player, () -> Msg.err(player, "You must be looking at a spawner."));
                return;
            }
            EntityType current = spawner.getSpawnedType();
            String label = current == null ? "empty" : current.name().toLowerCase(Locale.ROOT);
            Schedulers.entity(plugin, player, () -> Msg.value(player, "Spawner type:", label));
        });
    }

    // --- /spawner <type>: set the type -------------------------------------------

    private int setType(CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        String raw = StringArgumentType.getString(ctx, "type");
        EntityType type = SpawnerTypes.resolve(raw);
        if (type == null) {
            Msg.err(player, "Unknown entity type: " + raw + ".");
            return 0;
        }
        if (!type.isSpawnable()) {
            Msg.err(player, type.name().toLowerCase(Locale.ROOT) + " can't be placed in a spawner.");
            return 0;
        }
        Block target = player.getTargetBlockExact(REACH);
        if (target == null) {
            Msg.err(player, "You must be looking at a spawner.");
            return 0;
        }
        Location loc = target.getLocation();
        Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
            CreatureSpawner spawner = spawnerAt(loc);
            if (spawner == null) {
                Schedulers.entity(plugin, player, () -> Msg.err(player, "You must be looking at a spawner."));
                return;
            }
            spawner.setSpawnedType(type);
            spawner.update();
            Schedulers.entity(plugin, player, () ->
                    Msg.ok(player, "Spawner now spawns " + type.name().toLowerCase(Locale.ROOT) + "."));
        });
        return 1;
    }

    /**
     * Re-fetches the block state at {@code loc} and returns it as a
     * {@link CreatureSpawner}, or {@code null} if the block there is not a spawner. Must
     * be called on the block's own region thread.
     *
     * @param loc the targeted block's location
     * @return the spawner state, or {@code null} if the block is not a spawner
     */
    private static CreatureSpawner spawnerAt(Location loc) {
        BlockState state = loc.getBlock().getState();
        return state instanceof CreatureSpawner spawner ? spawner : null;
    }
}
