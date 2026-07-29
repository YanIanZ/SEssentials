package dev.iyanz.sessentials.module.adminbatch;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/**
 * Admin batch dispatcher: runs several commands in one shot from a single
 * {@code /batch} invocation. The argument is a {@code ;}-separated list of command
 * strings; each entry is trimmed, blanks are dropped, and a leading {@code /} is
 * stripped before dispatch. All entries are dispatched in order as the console
 * sender, so they run with full permissions regardless of who invoked {@code /batch}.
 *
 * <p>Folia-safe: command execution can touch worlds and entities, so the whole batch
 * is handed to the global region scheduler rather than run inline on the caller's
 * region thread.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class AdminBatchModule implements EssModule {

    /** Permission required to run {@code /batch}. */
    private static final String PERMISSION = "sessentials.batch";

    @Override
    public String name() {
        return "adminbatch";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("batch")
                .requires(s -> s.getSender().hasPermission(PERMISSION))
                .then(Commands.argument("commands", StringArgumentType.greedyString())
                        .executes(ctx -> dispatch(plugin, ctx)))
                .build(), "Run several ;-separated commands as the console"));
    }

    /**
     * Splits the greedy {@code commands} argument on {@code ;}, cleans each entry, and
     * schedules the whole cleaned batch to run as the console sender on the global
     * region scheduler. Reports the queued count (or an error for an empty batch) to
     * the invoking sender.
     */
    private int dispatch(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String raw = StringArgumentType.getString(ctx, "commands");

        List<String> cmds = new ArrayList<>();
        for (String part : raw.split(";")) {
            String cmd = part.trim();
            if (cmd.startsWith("/")) {
                cmd = cmd.substring(1).trim();
            }
            if (!cmd.isEmpty()) {
                cmds.add(cmd);
            }
        }

        if (cmds.isEmpty()) {
            Msg.err(sender, "No commands given.");
            return 0;
        }

        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            for (String c : cmds) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c);
            }
        });
        Msg.ok(sender, "Dispatched " + cmds.size() + " commands.");
        return 1;
    }
}
