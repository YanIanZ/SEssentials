package dev.iyanz.sessentials.module.inspect;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Registers {@code /blockinfo}: reports the type, location, light level and biome of
 * the block the sender is currently looking at, within {@value #MAX_DISTANCE} blocks.
 * Player-only, and only ever reads the sender's own line of sight, so it runs safely
 * on their own region thread without any scheduler hop.
 */
@SuppressWarnings("UnstableApiUsage")
final class BlockInfoCommand {

    private static final int MAX_DISTANCE = 10;

    private BlockInfoCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            var node = Commands.literal("blockinfo")
                    .requires(s -> s.getSender().hasPermission("sessentials.blockinfo"))
                    .executes(Cmds.playerExec(BlockInfoCommand::report))
                    .build();
            reg.register(node, "Show info about the block you're looking at");
        });
    }

    private static void report(Player player) {
        Block target = player.getTargetBlockExact(MAX_DISTANCE);
        if (target == null || target.isEmpty()) {
            Msg.err(player, "No block in sight within " + MAX_DISTANCE + " blocks.");
            return;
        }
        Msg.value(player, "Block:", InspectFormat.titleCase(target.getType().name()));
        Msg.value(player, "Location:", InspectFormat.location(target.getLocation()));
        Msg.value(player, "Light level:", String.valueOf(target.getLightLevel()));
        Msg.value(player, "Biome:", InspectFormat.titleCase(target.getBiome().getKey().getKey()));
    }
}
