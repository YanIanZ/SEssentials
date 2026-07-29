package dev.iyanz.sessentials.module.mutechat;

import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Server-wide chat mute: a single {@code /mutechat} toggle that silences public chat
 * for everyone at once, for cooling a heated room or quieting chat during an
 * announcement.
 *
 * <p>The mute is enforced by {@link MuteChatListener} against the shared
 * {@link MuteChatState} flag. Players holding {@code sessentials.mutechat.bypass}
 * (staff) may still chat while the mute is on. Toggling broadcasts the new state to
 * the whole server. State is session-only and resets on restart.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class MuteChatModule implements EssModule {

    @Override
    public String name() {
        return "mutechat";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new MuteChatListener(), plugin);

        plugin.commands(reg -> reg.register(Commands.literal("mutechat")
                .requires(s -> s.getSender().hasPermission("sessentials.mutechat"))
                .executes(ctx -> toggle(plugin, ctx))
                .build(), "Toggle server-wide chat mute"));
    }

    /**
     * Flips the global chat mute and announces the new state to everyone.
     *
     * @param plugin the owning plugin (for Folia-safe scheduling)
     * @param ctx    the command context
     * @return {@code 1} (always handled)
     */
    private int toggle(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        boolean nowMuted = MuteChatState.toggle();
        String message = nowMuted ? "Chat is now muted." : "Chat is now unmuted.";

        Msg.err(Bukkit.getConsoleSender(), message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            Schedulers.entity(plugin, player, () -> Msg.err(player, message));
        }
        return 1;
    }
}
