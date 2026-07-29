package dev.iyanz.sessentials.module.worldtools;

import java.util.List;
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
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

/**
 * Small world-inspection utilities: {@code /distance}, {@code /getpos}
 * ({@code /whereami}, {@code /coords}), {@code /findbiome} and {@code /fixlight}
 * ({@code /relight}).
 *
 * <p>Folia-safe: reading another online player's location ({@code /distance}) hops
 * onto that player's own region thread via {@link Schedulers#entity} before touching
 * it, the same pattern {@code module.teleport.Teleports} uses to read a teleport
 * anchor's location. {@code /findbiome} samples distant world columns via Paper's
 * region scheduler; see {@link BiomeFinder} for that Folia-safety writeup.
 * {@code /getpos} and {@code /fixlight} only ever touch the sender's own position,
 * which is already safe from a normal command executor (it runs on the sender's own
 * region thread).</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class WorldToolsModule implements EssModule {

    private SEssentialsPlugin plugin;

    @Override
    public String name() {
        return "worldtools";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.plugin = plugin;
        plugin.commands(reg -> {
            reg.register(Commands.literal("distance")
                    .requires(s -> s.getSender().hasPermission("sessentials.distance"))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .executes(this::distance))
                    .build(), "Report the distance to another online player");

            reg.register(Commands.literal("getpos")
                    .requires(s -> s.getSender().hasPermission("sessentials.getpos"))
                    .executes(Cmds.playerExec(this::getpos))
                    .build(), "Show your exact coordinates, world and facing", List.of("whereami", "coords"));

            reg.register(Commands.literal("findbiome")
                    .requires(s -> s.getSender().hasPermission("sessentials.findbiome"))
                    .then(Commands.argument("biome", StringArgumentType.word()).suggests(BiomeNames.SUGGESTIONS)
                            .executes(this::findBiome))
                    .build(), "Find the nearest block of a given biome");

            reg.register(Commands.literal("fixlight")
                    .requires(s -> s.getSender().hasPermission("sessentials.fixlight"))
                    .executes(Cmds.playerExec(this::fixLight))
                    .build(), "Resend your current chunk to nudge stray lighting", List.of("relight"));
        });
    }

    // --- distance ----------------------------------------------------------------

    private int distance(CommandContext<CommandSourceStack> ctx) {
        Player sender = Cmds.player(ctx);
        if (sender == null) {
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Msg.err(sender, name + " is not online.");
            return 0;
        }
        if (target.equals(sender)) {
            Msg.value(sender, "Distance to " + target.getName() + ":", "0 blocks");
            return 1;
        }

        // A normal command executor already runs on the sender's own region thread,
        // so capturing their location here is safe; reading the target's location is
        // only safe from the target's own region thread, so we hop there for that.
        Location senderLocation = sender.getLocation().clone();
        World senderWorld = sender.getWorld();

        Schedulers.entity(plugin, target, () -> {
            World targetWorld = target.getWorld();
            if (!targetWorld.equals(senderWorld)) {
                Schedulers.entity(plugin, sender, () -> Msg.err(sender,
                        target.getName() + " is in a different world (" + targetWorld.getName() + ")."));
                return;
            }
            double blocks = senderLocation.distance(target.getLocation());
            String formatted = String.format(Locale.ROOT, "%.1f blocks", blocks);
            Schedulers.entity(plugin, sender, () ->
                    Msg.value(sender, "Distance to " + target.getName() + ":", formatted));
        });
        return 1;
    }

    // --- getpos --------------------------------------------------------------------

    private void getpos(Player player) {
        Location loc = player.getLocation();
        Msg.value(player, "Position:",
                String.format(Locale.ROOT, "%.2f, %.2f, %.2f", loc.getX(), loc.getY(), loc.getZ()));
        Msg.value(player, "World:", player.getWorld().getName());
        Msg.value(player, "Facing:", Facing.cardinal(loc.getYaw())
                + String.format(Locale.ROOT, " (yaw %.1f, pitch %.1f)", loc.getYaw(), loc.getPitch()));
    }

    // --- findbiome -------------------------------------------------------------------

    private int findBiome(CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        String raw = StringArgumentType.getString(ctx, "biome");
        Biome biome = BiomeNames.resolve(raw);
        if (biome == null) {
            Msg.err(player, "Unknown biome: " + raw + ". Tab-complete to see valid names.");
            return 0;
        }
        BiomeFinder.search(plugin, player, biome);
        return 1;
    }

    // --- fixlight --------------------------------------------------------------------

    private void fixLight(Player player) {
        Location loc = player.getLocation();
        boolean resent = player.getWorld().refreshChunk(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        if (resent) {
            Msg.ok(player, "Resent your current chunk. Paper relights chunks automatically as they change.");
        } else {
            Msg.info(player, "Nothing to resend here. Paper relights chunks automatically as they change.");
        }
    }
}
