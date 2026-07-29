package dev.iyanz.sessentials.module.localchat;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Radius-limited local chat: an enabled server delivers a player's normal chat only to
 * other players within a configurable block radius (in the same world), while a leading
 * {@code !} escapes to server-wide "global" chat.
 *
 * <p>All of the delivery behaviour lives in {@link LocalChatListener}, which reads its
 * config live on every message; this class only wires the listener in and exposes
 * {@code /localchat reload} to re-read the config at runtime.</p>
 *
 * <p>Config keys (all under {@code local-chat}, read live so a reload applies at once):</p>
 * <pre>
 * local-chat:
 *   enabled: false   # master switch for the module (default false)
 *   radius: 100      # block radius within which local messages are delivered (default 100)
 * </pre>
 *
 * <p>Permissions:</p>
 * <ul>
 *   <li>{@code sessentials.localchat} — use {@code /localchat reload}.</li>
 *   <li>{@code sessentials.localchat.bypass} — the holder's messages are always global,
 *       regardless of the radius.</li>
 * </ul>
 */
@SuppressWarnings("UnstableApiUsage")
public final class LocalChatModule implements EssModule {

    @Override
    public String name() {
        return "localchat";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new LocalChatListener(plugin), plugin);

        plugin.commands(reg -> reg.register(Commands.literal("localchat")
                .requires(s -> s.getSender().hasPermission("sessentials.localchat"))
                .then(Commands.literal("reload").executes(ctx -> {
                    plugin.reloadConfig();
                    Msg.ok(ctx.getSource().getSender(), "Local chat config reloaded.");
                    return 1;
                }))
                .build(), "Radius-limited local chat"));
    }
}
