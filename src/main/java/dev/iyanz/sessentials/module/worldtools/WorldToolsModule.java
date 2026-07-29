package dev.iyanz.sessentials.module.worldtools;

import java.util.Locale;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

/**
 * World inspection helpers: {@code /distance}, {@code /getpos}, {@code /findbiome} and
 * a trivial {@code /fixlight}. Biome/block reads run on the sender's region thread
 * (the command executor already is), and the biome scan is bounded to stay cheap.
 */
@SuppressWarnings("UnstableApiUsage")
public final class WorldToolsModule implements EssModule {

    private static final int SCAN_RADIUS = 1500;
    private static final int SCAN_STEP = 64;

    @Override
    public String name() {
        return "worldtools";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            reg.register(Commands.literal("distance")
                    .requires(s -> s.getSender().hasPermission("sessentials.distance"))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .executes(this::distance))
                    .build(), "Distance to another player");

            reg.register(Commands.literal("getpos")
                    .requires(s -> s.getSender().hasPermission("sessentials.getpos"))
                    .executes(Cmds.playerExec(this::getpos))
                    .build(), "Show your coordinates", java.util.List.of("whereami", "coords"));

            reg.register(Commands.literal("findbiome")
                    .requires(s -> s.getSender().hasPermission("sessentials.findbiome"))
                    .then(Commands.argument("biome", StringArgumentType.word()).suggests(this::biomeSuggestions)
                            .executes(this::findbiome))
                    .build(), "Find the nearest biome");

            reg.register(Commands.literal("fixlight")
                    .requires(s -> s.getSender().hasPermission("sessentials.fixlight"))
                    .executes(Cmds.playerExec(p ->
                            Msg.info(p, "Paper relights chunks automatically — nothing to fix.")))
                    .build(), "Lighting info", java.util.List.of("relight"));
        });
    }

    private int distance(CommandContext<CommandSourceStack> ctx) {
        Player sender = Cmds.player(ctx);
        if (sender == null) {
            return 0;
        }
        Player target = Bukkit.getPlayerExact(StringArgumentType.getString(ctx, "player"));
        if (target == null) {
            Msg.err(sender, "That player is not online.");
            return 0;
        }
        if (!target.getWorld().equals(sender.getWorld())) {
            Msg.err(sender, target.getName() + " is in another world.");
            return 0;
        }
        double d = sender.getLocation().distance(target.getLocation());
        Msg.value(sender, "Distance to " + target.getName() + ":", String.format("%.1f blocks", d));
        return 1;
    }

    private void getpos(Player p) {
        Location l = p.getLocation();
        Msg.value(p, "Position:", String.format("%.1f, %.1f, %.1f", l.getX(), l.getY(), l.getZ()));
        Msg.value(p, "World:", l.getWorld().getName() + "  " + facing(l.getYaw()));
    }

    private int findbiome(CommandContext<CommandSourceStack> ctx) {
        Player p = Cmds.player(ctx);
        if (p == null) {
            return 0;
        }
        String wanted = StringArgumentType.getString(ctx, "biome").toLowerCase(Locale.ROOT);
        Location origin = p.getLocation();
        int oy = origin.getBlockY();
        Location best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx += SCAN_STEP) {
            for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz += SCAN_STEP) {
                int x = origin.getBlockX() + dx;
                int z = origin.getBlockZ() + dz;
                Biome biome = origin.getWorld().getBiome(x, oy, z);
                if (biome.getKey().getKey().equals(wanted)) {
                    double dist = (double) dx * dx + (double) dz * dz;
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = new Location(origin.getWorld(), x, oy, z);
                    }
                }
            }
        }
        if (best == null) {
            Msg.err(p, "No '" + wanted + "' biome found within " + SCAN_RADIUS + " blocks.");
            return 0;
        }
        Msg.value(p, "Nearest " + wanted + ":", String.format("%d, %d, %d (%.0fm)",
                best.getBlockX(), best.getBlockY(), best.getBlockZ(), Math.sqrt(bestDist)));
        return 1;
    }

    private java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> biomeSuggestions(
            CommandContext<CommandSourceStack> ctx, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();
        for (Biome biome : Registry.BIOME) {
            String key = biome.getKey().getKey();
            if (key.startsWith(remaining)) {
                builder.suggest(key);
            }
        }
        return builder.buildFuture();
    }

    private static String facing(float yaw) {
        String[] dirs = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        int i = Math.round(yaw / 45f) & 7;
        return "facing " + dirs[i];
    }
}
