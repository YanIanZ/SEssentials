package dev.iyanz.sessentials.module.lockdown;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Server-wide lockdown: an operator toggle that both blocks new logins and clears the
 * server of non-staff players, for maintenance or incident response.
 *
 * <p>{@code /lockdown [reason]} flips the shared {@link LockdownState} flag. Turning it
 * on immediately kicks every online player who lacks
 * {@code sessentials.lockdown.bypass} and, via {@link LockdownListener}, refuses all
 * subsequent login attempts until the lockdown is lifted. Staff with the bypass
 * permission stay connected and can toggle the lockdown back off. {@code /kickall
 * [reason]} performs the same mass kick of non-staff players but leaves the lockdown
 * flag untouched, for a quick sweep without sealing the server.</p>
 *
 * <p>Both commands require {@code sessentials.lockdown}. Lockdown state is session-only
 * and resets on restart.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class LockdownModule implements EssModule {

    private static final String PERM = "sessentials.lockdown";
    private static final String BYPASS = "sessentials.lockdown.bypass";

    @Override
    public String name() {
        return "lockdown";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new LockdownListener(), plugin);

        plugin.commands(reg -> {
            reg.register(Commands.literal("lockdown")
                    .requires(s -> s.getSender().hasPermission(PERM))
                    .executes(ctx -> lockdown(plugin, ctx, null))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                            .executes(ctx -> lockdown(plugin, ctx, StringArgumentType.getString(ctx, "reason"))))
                    .build(), "Toggle server lockdown (blocks logins + kicks non-staff)");

            reg.register(Commands.literal("kickall")
                    .requires(s -> s.getSender().hasPermission(PERM))
                    .executes(ctx -> kickall(plugin, ctx, null))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                            .executes(ctx -> kickall(plugin, ctx, StringArgumentType.getString(ctx, "reason"))))
                    .build(), "Kick all non-staff players without changing lockdown state");
        });
    }

    /**
     * Toggles the server lockdown, kicking non-staff players when it turns on, and
     * reports the new state to the actor.
     *
     * @param plugin the owning plugin (for Folia-safe scheduling)
     * @param ctx    the command context
     * @param reason the operator-supplied reason, or {@code null} for the default
     * @return {@code 1} (always handled)
     */
    private int lockdown(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx, String reason) {
        CommandSender actor = ctx.getSource().getSender();
        if (LockdownState.active()) {
            LockdownState.deactivate();
            Msg.info(actor, "Server lockdown lifted. Logins are open again.");
            return 1;
        }
        String resolved = resolveReason(reason);
        LockdownState.activate(resolved);
        kickNonStaff(plugin, resolved);
        Msg.ok(actor, "Server locked down: " + resolved);
        return 1;
    }

    /**
     * Kicks every non-staff player without touching the lockdown flag.
     *
     * @param plugin the owning plugin (for Folia-safe scheduling)
     * @param ctx    the command context
     * @param reason the operator-supplied reason, or {@code null} for the default
     * @return {@code 1} (always handled)
     */
    private int kickall(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx, String reason) {
        String resolved = resolveReason(reason);
        kickNonStaff(plugin, resolved);
        Msg.ok(ctx.getSource().getSender(), "Kicked all non-staff players.");
        return 1;
    }

    /**
     * Kicks every online player lacking the bypass permission, hopping to each
     * player's region thread as Folia requires for entity actions.
     *
     * @param plugin the owning plugin
     * @param reason the kick message text
     */
    private static void kickNonStaff(SEssentialsPlugin plugin, String reason) {
        Component message = Component.text(reason);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(BYPASS)) {
                continue; // staff stay connected
            }
            Schedulers.entity(plugin, player, () -> player.kick(message));
        }
    }

    /** @return the given reason if non-blank, otherwise the default lockdown reason. */
    private static String resolveReason(String reason) {
        return (reason != null && !reason.isBlank()) ? reason : LockdownState.DEFAULT_REASON;
    }
}
