package dev.iyanz.sessentials.module.antispam;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Curbs chat spam with a per-player message cooldown and an optional block on repeating
 * the same message twice in a row. The rules live in {@link AntiSpamListener} and are
 * driven by the {@code anti-spam.*} config section; this class only wires the listener in
 * and exposes {@code /antispam reload} to re-read the config at runtime.
 *
 * <p>Config keys (all under {@code anti-spam}, read live so a reload applies at once):</p>
 * <pre>
 * anti-spam:
 *   enabled: true          # master switch for the module
 *   cooldown-seconds: 2    # minimum gap between a player's accepted messages
 *   block-repeat: true     # reject a message equal to the sender's previous one
 * </pre>
 */
@SuppressWarnings("UnstableApiUsage")
public final class AntiSpamModule implements EssModule {

    @Override
    public String name() {
        return "antispam";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new AntiSpamListener(plugin), plugin);
        plugin.commands(reg -> reg.register(Commands.literal("antispam")
                .requires(s -> s.getSender().hasPermission("sessentials.antispam"))
                .then(Commands.literal("reload").executes(ctx -> {
                    plugin.reloadConfig();
                    Msg.ok(ctx.getSource().getSender(), "Anti-spam config reloaded.");
                    return 1;
                }))
                .build(), "Chat anti-spam (cooldown + repeat block)"));
    }
}
