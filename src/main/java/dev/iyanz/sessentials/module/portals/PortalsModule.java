package dev.iyanz.sessentials.module.portals;

import java.util.Collection;
import java.util.Locale;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Region-based teleport portals: any bounding box a staff member draws with the
 * portal wand teleports a player who walks into it to a configured destination
 * (a warp, or the server spawn).
 *
 * <p>Workflow: {@code /portal wand} gives a {@link Material#GOLDEN_AXE GOLDEN_AXE}
 * selection tool ({@link PortalWand}); left/right-clicking blocks with it sets the
 * two corners of a box ({@link PortalWandListener}, tracked per-player in
 * {@link PortalSelections}); {@code /portal create <name> <destination>} saves the
 * caller's current selection as a named portal ({@link PortalRegistry}); and a
 * {@link PlayerMoveEvent} listener ({@link PortalMoveListener}) teleports any player
 * who steps inside a portal's box to its resolved destination
 * ({@link PortalDestinations}).</p>
 *
 * <p>Every subcommand requires {@code sessentials.portals}. Folia-safe: enforcement
 * teleports go through {@link Player#teleportAsync} on the moving player's own region
 * thread, and self-affecting commands (giving the wand, creating a portal from the
 * caller's own selection) run on the caller's own thread already.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class PortalsModule implements EssModule {

    private static final String PERMISSION = "sessentials.portals";

    private final PortalRegistry registry = new PortalRegistry();
    private final PortalSelections selections = new PortalSelections();

    @Override
    public String name() {
        return "portals";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        registry.load(plugin);

        plugin.getServer().getPluginManager().registerEvents(new PortalWandListener(plugin, selections), plugin);
        plugin.getServer().getPluginManager().registerEvents(new PortalMoveListener(plugin, registry), plugin);

        plugin.commands(reg -> reg.register(buildCommand(plugin), "Region-based teleport portals"));
    }

    /** Builds the full {@code /portal} command tree. */
    private LiteralCommandNode<CommandSourceStack> buildCommand(SEssentialsPlugin plugin) {
        return Commands.literal("portal")
                .requires(s -> s.getSender().hasPermission(PERMISSION))
                .executes(ctx -> {
                    Msg.err(ctx.getSource().getSender(), "Usage: /portal <wand|create|delete|list>.");
                    return 0;
                })
                .then(Commands.literal("wand")
                        .executes(Cmds.playerExec(p -> giveWand(plugin, p))))
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .then(Commands.argument("destination", StringArgumentType.word())
                                        .suggests(PortalSuggestions.destinations(plugin))
                                        .executes(ctx -> create(plugin, ctx)))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(PortalSuggestions.names(registry))
                                .executes(ctx -> delete(plugin, ctx))))
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            list(ctx.getSource().getSender());
                            return 1;
                        }))
                .build();
    }

    /** Gives {@code player} a fresh portal wand. */
    private void giveWand(SEssentialsPlugin plugin, Player player) {
        player.getInventory().addItem(PortalWand.create(plugin));
        Msg.ok(player, "You received the portal wand. Left/right-click blocks to select a region.");
    }

    /**
     * Creates (or overwrites) a portal named {@code <name>} from the caller's current
     * two-point selection, teleporting into {@code <destination>} (a warp name, or
     * {@code "spawn"}).
     */
    private int create(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "name").toLowerCase(Locale.ROOT);
        String destination = StringArgumentType.getString(ctx, "destination").toLowerCase(Locale.ROOT);

        Location[] selection = selections.get(player.getUniqueId());
        if (selection == null || selection[0] == null || selection[1] == null) {
            Msg.err(player, "Select two positions with the portal wand first (/portal wand).");
            return 0;
        }
        Location pos1 = selection[0];
        Location pos2 = selection[1];
        if (pos1.getWorld() == null || pos2.getWorld() == null || !pos1.getWorld().equals(pos2.getWorld())) {
            Msg.err(player, "Both selected positions must be in the same world.");
            return 0;
        }
        if (!PortalDestinations.isValid(plugin, destination)) {
            Msg.err(player, "\"" + destination + "\" is not a warp name (or \"spawn\").");
            return 0;
        }

        Portal portal = new Portal(name, pos1.getWorld().getName(),
                Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()),
                Math.max(pos1.getX(), pos2.getX()), Math.max(pos1.getY(), pos2.getY()), Math.max(pos1.getZ(), pos2.getZ()),
                destination);
        registry.save(plugin, portal);
        Msg.ok(player, "Portal \"" + name + "\" created, sending players to \"" + destination + "\".");
        return 1;
    }

    /** Deletes the portal named {@code <name>}, or errors if it doesn't exist. */
    private int delete(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "name").toLowerCase(Locale.ROOT);
        if (!registry.exists(name)) {
            Msg.err(sender, "No portal named \"" + name + "\".");
            return 0;
        }
        registry.delete(plugin, name);
        Msg.ok(sender, "Portal \"" + name + "\" deleted.");
        return 1;
    }

    /** Lists every portal's name and destination. */
    private void list(CommandSender sender) {
        Collection<Portal> portals = registry.all();
        if (portals.isEmpty()) {
            Msg.info(sender, "There are no portals set.");
            return;
        }
        Msg.info(sender, "Portals:");
        for (Portal portal : portals) {
            Msg.value(sender, portal.name() + ":", portal.destination());
        }
    }
}
