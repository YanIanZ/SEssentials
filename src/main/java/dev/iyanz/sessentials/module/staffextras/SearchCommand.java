package dev.iyanz.sessentials.module.staffextras;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * {@code /search <item>} ({@code sessentials.search}) — counts how many of a material
 * every online player is carrying and reports the per-player totals to the sender.
 *
 * <p>Folia-safe by construction: another player's inventory may only be read on that
 * player's region thread, so the command snapshots the online list, hops to each
 * target with {@link Schedulers#entity}, counts there, and accumulates into a
 * concurrent map. An atomic countdown fires the report after the last hop completes;
 * the report itself is delivered on the sender's own region thread (or directly for
 * the console, whose {@code sendMessage} is thread-safe).</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class SearchCommand {

    /** Frequently searched-for materials offered as tab suggestions. */
    private static final List<String> COMMON_MATERIALS = List.of(
            "diamond", "diamond_block", "emerald", "netherite_ingot", "netherite_block",
            "ancient_debris", "gold_ingot", "iron_ingot", "gold_block", "iron_block",
            "tnt", "ender_pearl", "ender_chest", "elytra", "totem_of_undying",
            "enchanted_golden_apple", "golden_apple", "shulker_box", "obsidian",
            "bedrock", "spawner", "beacon", "nether_star", "end_crystal", "trident",
            "netherite_sword", "diamond_sword", "diamond_pickaxe", "wither_skeleton_skull");

    /** Suggests the common materials, filtered by the typed prefix. */
    private static final SuggestionProvider<CommandSourceStack> SUGGESTIONS = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (String name : COMMON_MATERIALS) {
            if (name.startsWith(remaining)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    };

    private SearchCommand() {
    }

    /**
     * Registers {@code /search}.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("search")
                .requires(s -> s.getSender().hasPermission("sessentials.search"))
                .then(Commands.argument("item", StringArgumentType.word())
                        .suggests(SUGGESTIONS)
                        .executes(ctx -> search(plugin, ctx)))
                .build(), "Count how many of an item every online player holds"));
    }

    /** Resolves the material, fans out one region hop per online player, then reports. */
    private static int search(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String raw = StringArgumentType.getString(ctx, "item");

        Material material = resolve(raw);
        if (material == null) {
            Msg.err(sender, raw + " is not a known item.");
            return 0;
        }

        List<Player> targets = List.copyOf(Bukkit.getOnlinePlayers());
        if (targets.isEmpty()) {
            Msg.info(sender, "Nobody is online to search.");
            return 1;
        }

        Map<String, Integer> counts = new ConcurrentHashMap<>();
        AtomicInteger remaining = new AtomicInteger(targets.size());
        for (Player target : targets) {
            ScheduledTask hop = Schedulers.entity(plugin, target, () -> {
                int held = count(target, material);
                if (held > 0) {
                    counts.put(target.getName(), held);
                }
                if (remaining.decrementAndGet() == 0) {
                    report(plugin, sender, material, counts);
                }
            });
            // A retired entity never runs its task; count it as holding nothing.
            if (hop == null && remaining.decrementAndGet() == 0) {
                report(plugin, sender, material, counts);
            }
        }
        return 1;
    }

    /**
     * Resolves user input to a {@link Material}, accepting plain names, legacy-style
     * names and {@code minecraft:} keys.
     *
     * @param raw the typed material name
     * @return the material, or {@code null} if nothing matches
     */
    private static Material resolve(String raw) {
        Material material = Material.matchMaterial(raw);
        if (material != null) {
            return material;
        }
        NamespacedKey key = NamespacedKey.fromString(raw.toLowerCase(Locale.ROOT));
        return key == null ? null : Registry.MATERIAL.get(key);
    }

    /**
     * Sums how many of {@code material} the player carries across their whole
     * inventory (storage, armor and off hand). Must run on the player's region thread.
     *
     * @param player   the player being searched
     * @param material the material to count
     * @return the total item count
     */
    private static int count(Player player, Material material) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == material) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    /**
     * Delivers the per-player totals to the sender — on the sender's region thread
     * for players, directly for the console.
     *
     * @param plugin   the owning plugin
     * @param sender   who asked for the search
     * @param material the searched material
     * @param counts   player name to item count, only players holding at least one
     */
    private static void report(SEssentialsPlugin plugin, CommandSender sender, Material material, Map<String, Integer> counts) {
        Runnable deliver = () -> {
            String item = material.name().toLowerCase(Locale.ROOT);
            if (counts.isEmpty()) {
                Msg.info(sender, "No online player is holding " + item + ".");
                return;
            }
            Msg.info(sender, "Players holding " + item + ":");
            int total = 0;
            for (Map.Entry<String, Integer> entry : counts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .toList()) {
                Msg.value(sender, entry.getKey() + ":", "x" + entry.getValue());
                total += entry.getValue();
            }
            Msg.value(sender, "Total:", "x" + total);
        };
        if (sender instanceof Player player) {
            Schedulers.entity(plugin, player, deliver);
        } else {
            deliver.run();
        }
    }
}
