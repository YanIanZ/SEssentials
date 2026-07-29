package dev.iyanz.sessentials.module.savedinv;

import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

/**
 * Per-player named inventory snapshots: save your current inventory under a name,
 * restore it later, delete it, or list your saved snapshots.
 *
 * <p>Commands: {@code /saveinv <name>}, {@code /loadinv <name>}, {@code /delinv <name>}
 * and {@code /listinv}, all gated by the single {@code sessentials.savedinv}
 * permission. A snapshot covers the sender's full inventory — the 36 storage slots,
 * the 4 armour slots and the off-hand — see {@link SavedInvService} for the storage
 * convention. Snapshot names are case-insensitive (always lower-cased).</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class SavedInvModule implements EssModule {

    private static final String PERMISSION = "sessentials.savedinv";

    @Override
    public String name() {
        return "savedinv";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            reg.register(
                    Commands.literal("saveinv")
                            .requires(s -> s.getSender().hasPermission(PERMISSION))
                            .then(Commands.argument("name", StringArgumentType.word())
                                    .executes(ctx -> saveInv(plugin, ctx)))
                            .build(),
                    "Save a snapshot of your current inventory");

            reg.register(
                    Commands.literal("loadinv")
                            .requires(s -> s.getSender().hasPermission(PERMISSION))
                            .then(Commands.argument("name", StringArgumentType.word())
                                    .suggests(SavedInvSuggestions.of(plugin))
                                    .executes(ctx -> loadInv(plugin, ctx)))
                            .build(),
                    "Restore a saved inventory snapshot (overwrites your current inventory)");

            reg.register(
                    Commands.literal("delinv")
                            .requires(s -> s.getSender().hasPermission(PERMISSION))
                            .then(Commands.argument("name", StringArgumentType.word())
                                    .suggests(SavedInvSuggestions.of(plugin))
                                    .executes(ctx -> deleteInv(plugin, ctx)))
                            .build(),
                    "Delete a saved inventory snapshot");

            reg.register(
                    Commands.literal("listinv")
                            .requires(s -> s.getSender().hasPermission(PERMISSION))
                            .executes(Cmds.playerExec(p -> listInv(plugin, p)))
                            .build(),
                    "List your saved inventory snapshots");
        });
    }

    /** Snapshots the sender's inventory as the named save, overwriting any existing one. */
    private static int saveInv(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "name").toLowerCase(Locale.ROOT);
        boolean overwritten = SavedInvService.exists(plugin, player.getUniqueId(), name);
        SavedInvService.save(plugin, player, name);
        Msg.ok(player, overwritten
                ? "Overwrote saved inventory \"" + name + "\" with your current inventory."
                : "Saved your inventory as \"" + name + "\".");
        return 1;
    }

    /** Restores the sender's named snapshot, overwriting their current inventory. */
    private static int loadInv(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "name").toLowerCase(Locale.ROOT);
        if (!SavedInvService.load(plugin, player, name)) {
            Msg.err(player, "You have no saved inventory named \"" + name + "\".");
            return 0;
        }
        Msg.ok(player, "Loaded inventory \"" + name + "\" — your current inventory was overwritten.");
        return 1;
    }

    /** Deletes the sender's named snapshot, or errors if it doesn't exist. */
    private static int deleteInv(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "name").toLowerCase(Locale.ROOT);
        if (!SavedInvService.exists(plugin, player.getUniqueId(), name)) {
            Msg.err(player, "You have no saved inventory named \"" + name + "\".");
            return 0;
        }
        SavedInvService.delete(plugin, player.getUniqueId(), name);
        Msg.ok(player, "Deleted saved inventory \"" + name + "\".");
        return 1;
    }

    /** Lists the sender's saved snapshot names. */
    private static void listInv(SEssentialsPlugin plugin, Player player) {
        List<String> names = SavedInvService.names(plugin, player.getUniqueId());
        if (names.isEmpty()) {
            Msg.info(player, "You have no saved inventories.");
            return;
        }
        Msg.info(player, "Saved inventories: " + String.join(", ", names));
    }
}
