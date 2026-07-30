package dev.iyanz.sessentials.module.buildtools;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Admin build tools: {@code /tree} (grow a tree where you look),
 * {@code /replaceblock} (swap one block type for another around you),
 * {@code /scan} (count and locate a block type near you), {@code /point}
 * (a particle pointer along your line of sight) and {@code /silence}
 * (hide your own join/leave broadcasts).
 *
 * <p>Folia-safe: every block or world mutation is dispatched to the region thread
 * that owns the <em>target location</em> via Paper's region scheduler — see
 * {@link TreePlanter} and {@link BlockReplacer}. {@code /scan} and {@code /point}
 * only read blocks / spawn particles near the sender and therefore run inline on
 * the sender's own region thread, which a command executor is already on.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class BuildToolsModule implements EssModule {

    private SEssentialsPlugin plugin;
    private SilenceFlags silenceFlags;

    @Override
    public String name() {
        return "buildtools";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.plugin = plugin;
        this.silenceFlags = new SilenceFlags(plugin.stores().get("buildtools"));

        plugin.commands(reg -> {
            reg.register(Commands.literal("tree")
                    .requires(s -> s.getSender().hasPermission("sessentials.tree"))
                    .executes(Cmds.playerExec(p -> TreePlanter.plant(plugin, p, null)))
                    .then(Commands.argument("type", StringArgumentType.word())
                            .suggests(TreePlanter.TREE_TYPES)
                            .executes(this::tree))
                    .build(), "Grow a tree at the block you are looking at");

            reg.register(Commands.literal("replaceblock")
                    .requires(s -> s.getSender().hasPermission("sessentials.replaceblock"))
                    .then(Commands.argument("from", StringArgumentType.word())
                            .suggests(BlockMaterials.BLOCKS)
                            .then(Commands.argument("to", StringArgumentType.word())
                                    .suggests(BlockMaterials.BLOCKS)
                                    .executes(ctx -> replaceBlock(ctx, BlockReplacer.DEFAULT_RADIUS))
                                    .then(Commands.argument("radius",
                                                    IntegerArgumentType.integer(1, BlockReplacer.MAX_RADIUS))
                                            .executes(ctx -> replaceBlock(ctx,
                                                    IntegerArgumentType.getInteger(ctx, "radius"))))))
                    .build(), "Replace one block type with another around you");

            reg.register(Commands.literal("scan")
                    .requires(s -> s.getSender().hasPermission("sessentials.scan"))
                    .then(Commands.argument("block", StringArgumentType.word())
                            .suggests(BlockMaterials.BLOCKS)
                            .executes(ctx -> scan(ctx, BlockScanner.DEFAULT_RADIUS))
                            .then(Commands.argument("radius",
                                            IntegerArgumentType.integer(1, BlockScanner.MAX_RADIUS))
                                    .executes(ctx -> scan(ctx,
                                            IntegerArgumentType.getInteger(ctx, "radius")))))
                    .build(), "Count a block type near you and locate the nearest one");

            reg.register(Commands.literal("point")
                    .requires(s -> s.getSender().hasPermission("sessentials.point"))
                    .executes(Cmds.playerExec(ParticlePointer::point))
                    .build(), "Draw a particle pointer along your line of sight");

            reg.register(Commands.literal("silence")
                    .requires(s -> s.getSender().hasPermission("sessentials.silence"))
                    .executes(Cmds.playerExec(this::silence))
                    .build(), "Toggle hiding your own join and leave messages");
        });

        plugin.getServer().getPluginManager().registerEvents(new SilenceListener(silenceFlags), plugin);
    }

    /** Runs {@code /tree <type>} for the sending player. */
    private int tree(CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        TreePlanter.plant(plugin, player, StringArgumentType.getString(ctx, "type"));
        return 1;
    }

    /** Runs {@code /replaceblock <from> <to> [radius]} for the sending player. */
    private int replaceBlock(CommandContext<CommandSourceStack> ctx, int radius) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        Material from = BlockMaterials.parse(player, StringArgumentType.getString(ctx, "from"));
        Material to = BlockMaterials.parse(player, StringArgumentType.getString(ctx, "to"));
        if (from == null || to == null) {
            return 0;
        }
        if (from == to) {
            Msg.err(player, "The two block types must differ.");
            return 0;
        }
        BlockReplacer.replace(plugin, player, from, to, radius);
        return 1;
    }

    /** Runs {@code /scan <block> [radius]} for the sending player. */
    private int scan(CommandContext<CommandSourceStack> ctx, int radius) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        Material target = BlockMaterials.parse(player, StringArgumentType.getString(ctx, "block"));
        if (target == null) {
            return 0;
        }
        BlockScanner.scan(player, target, radius);
        return 1;
    }

    /** Toggles the sender's own join/leave broadcast suppression. */
    private void silence(Player player) {
        if (silenceFlags.toggle(player.getUniqueId())) {
            Msg.ok(player, "Your join and leave messages are now hidden.");
        } else {
            Msg.ok(player, "Your join and leave messages are now shown.");
        }
    }
}
