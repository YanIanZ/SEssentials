package dev.iyanz.sessentials.module.itemadmin;

import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

/**
 * {@code /trim <pattern> <material>}: applies an armor trim to the held armor
 * piece. Pattern and material names are resolved against the vanilla
 * {@link Registry#TRIM_PATTERN} / {@link Registry#TRIM_MATERIAL} registries via
 * their {@code minecraft:} keys, with curated tab suggestions for every vanilla
 * name. Errors clearly when the held item is not trimmable armor or a name is
 * unknown.
 */
@SuppressWarnings("UnstableApiUsage")
final class TrimCommand {

    /** Vanilla trim pattern names, for tab suggestions. */
    private static final List<String> PATTERNS = List.of(
            "bolt", "coast", "dune", "eye", "flow", "host", "raiser", "rib",
            "sentry", "shaper", "silence", "snout", "spire", "tide", "vex",
            "ward", "wayfinder", "wild");

    /** Vanilla trim material names, for tab suggestions. */
    private static final List<String> MATERIALS = List.of(
            "amethyst", "copper", "diamond", "emerald", "gold", "iron",
            "lapis", "netherite", "quartz", "redstone", "resin");

    /** Suggests the curated vanilla trim pattern names, filtered by prefix. */
    private static final SuggestionProvider<CommandSourceStack> PATTERN_SUGGESTIONS =
            suggestions(PATTERNS);

    /** Suggests the curated vanilla trim material names, filtered by prefix. */
    private static final SuggestionProvider<CommandSourceStack> MATERIAL_SUGGESTIONS =
            suggestions(MATERIALS);

    private TrimCommand() {
    }

    /**
     * Registers {@code /trim}.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(
                Commands.literal("trim")
                        .requires(s -> s.getSender().hasPermission("sessentials.trim"))
                        .then(Commands.argument("pattern", StringArgumentType.word())
                                .suggests(PATTERN_SUGGESTIONS)
                                .then(Commands.argument("material", StringArgumentType.word())
                                        .suggests(MATERIAL_SUGGESTIONS)
                                        .executes(TrimCommand::trim)))
                        .build(),
                "Apply an armor trim to the armor piece in your hand"));
    }

    @SuppressWarnings("deprecation") // Registry.TRIM_PATTERN/TRIM_MATERIAL: brief-mandated lookup path
    private static int trim(CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        ItemStack hand = Held.mainHandOrError(player);
        if (hand == null) {
            return 0;
        }
        if (!(hand.getItemMeta() instanceof ArmorMeta meta)) {
            Msg.err(player, "The item in your hand is not trimmable armor.");
            return 0;
        }
        String patternName = StringArgumentType.getString(ctx, "pattern").toLowerCase(Locale.ROOT);
        String materialName = StringArgumentType.getString(ctx, "material").toLowerCase(Locale.ROOT);
        TrimPattern pattern = lookup(Registry.TRIM_PATTERN, patternName);
        if (pattern == null) {
            Msg.err(player, "Unknown trim pattern '" + patternName + "'.");
            return 0;
        }
        TrimMaterial material = lookup(Registry.TRIM_MATERIAL, materialName);
        if (material == null) {
            Msg.err(player, "Unknown trim material '" + materialName + "'.");
            return 0;
        }
        meta.setTrim(new ArmorTrim(material, pattern));
        hand.setItemMeta(meta);
        Msg.ok(player, "Applied " + materialName + " " + patternName + " trim.");
        return 1;
    }

    /**
     * Resolves a registry entry from a plain lowercase name under the
     * {@code minecraft:} namespace, tolerating names that are not even valid keys.
     *
     * @param registry the registry to query
     * @param name     the plain entry name (e.g. {@code sentry})
     * @param <T>      the registry entry type
     * @return the entry, or {@code null} if the name is invalid or unregistered
     */
    private static <T extends org.bukkit.Keyed> T lookup(Registry<T> registry, String name) {
        try {
            return registry.get(NamespacedKey.minecraft(name));
        } catch (IllegalArgumentException invalidKey) {
            return null;
        }
    }

    private static SuggestionProvider<CommandSourceStack> suggestions(List<String> names) {
        return (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            for (String name : names) {
                if (name.startsWith(remaining)) {
                    builder.suggest(name);
                }
            }
            return builder.buildFuture();
        };
    }
}
