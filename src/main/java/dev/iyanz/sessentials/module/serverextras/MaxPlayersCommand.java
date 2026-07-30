package dev.iyanz.sessentials.module.serverextras;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/**
 * Registers {@code /maxplayers <n>}: changes the server's player slot count live via
 * {@link Bukkit#setMaxPlayers(int)} — no restart, no {@code server.properties} edit
 * (the change is runtime-only and reverts on restart).
 *
 * <p>The slot count is a server-global value, so the mutation runs on the global
 * region scheduler (Folia-safe). The Paper API call is additionally guarded at
 * runtime: should the method be absent or unsupported on the running server build,
 * the sender is told it is unsupported instead of the command erroring out.</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class MaxPlayersCommand {

    /** Permission required to change the slot count. */
    private static final String PERMISSION = "sessentials.maxplayers";

    private MaxPlayersCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("maxplayers")
                .requires(s -> s.getSender().hasPermission(PERMISSION))
                .then(Commands.argument("slots", IntegerArgumentType.integer(1))
                        .executes(ctx -> apply(plugin, ctx)))
                .build(), "Set the server's max player count live"));
    }

    /**
     * Schedules the slot-count change on the global region scheduler.
     *
     * @param plugin the owning plugin (for scheduler access)
     * @param ctx    the command context (carries the {@code slots} argument)
     * @return always 1; the outcome is reported asynchronously from the global thread
     */
    private static int apply(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        int slots = IntegerArgumentType.getInteger(ctx, "slots");
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            try {
                Bukkit.setMaxPlayers(slots);
                Msg.ok(sender, "Max players set to " + slots + " (until restart).");
            } catch (NoSuchMethodError | UnsupportedOperationException e) {
                Msg.err(sender, "Changing max players live is not supported on this server build.");
            }
        });
        return 1;
    }
}
