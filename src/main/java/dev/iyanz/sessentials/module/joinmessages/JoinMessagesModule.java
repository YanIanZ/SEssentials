package dev.iyanz.sessentials.module.joinmessages;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Replaces the vanilla join/quit broadcasts with configurable, MiniMessage-based lines,
 * including a distinct first-join welcome. The behaviour lives in
 * {@link JoinMessagesListener} and is driven entirely by the {@code join-messages.*}
 * config section, read live on every event so a reload applies at once; this class only
 * wires the listener in and exposes {@code /joinmessages reload} to re-read the config.
 *
 * <p>Config keys (all under {@code join-messages}, {@code %player%} is substituted with
 * the player's name and the result is parsed as MiniMessage):</p>
 * <pre>
 * join-messages:
 *   enabled: false                              # master switch; when off, vanilla messages stay
 *   silent: false                               # when on, suppress the join/quit broadcast entirely
 *   join: "&lt;yellow&gt;%player% &lt;gray&gt;joined."          # message for a returning player joining
 *   quit: "&lt;yellow&gt;%player% &lt;gray&gt;left."            # message for a player leaving
 *   first-join: "&lt;gold&gt;Welcome &lt;yellow&gt;%player%&lt;gold&gt;!" # message for a player's very first join
 * </pre>
 */
@SuppressWarnings("UnstableApiUsage")
public final class JoinMessagesModule implements EssModule {

    @Override
    public String name() {
        return "joinmessages";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new JoinMessagesListener(plugin), plugin);
        plugin.commands(reg -> reg.register(Commands.literal("joinmessages")
                .requires(s -> s.getSender().hasPermission("sessentials.joinmessages"))
                .then(Commands.literal("reload").executes(ctx -> {
                    plugin.reloadConfig();
                    Msg.ok(ctx.getSource().getSender(), "Join-messages config reloaded.");
                    return 1;
                }))
                .build(), "Custom join/quit messages"));
    }
}
