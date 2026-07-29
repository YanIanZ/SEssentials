package dev.iyanz.sessentials.module.cmdcontrol;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Per-command cooldowns and costs, enforced via {@link CmdControlListener} and driven
 * by the {@code command-control} config section (see {@link CmdControlSettings}).
 * {@code /cmdcontrol reload} re-reads the config at runtime.
 */
@SuppressWarnings("UnstableApiUsage")
public final class CmdControlModule implements EssModule {

    private CmdControlSettings settings;

    @Override
    public String name() {
        return "cmdcontrol";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.settings = CmdControlSettings.load(plugin.getConfig());
        plugin.getServer().getPluginManager().registerEvents(new CmdControlListener(plugin, settings), plugin);
        plugin.commands(reg -> reg.register(Commands.literal("cmdcontrol")
                .requires(s -> s.getSender().hasPermission("sessentials.cmdcontrol.admin"))
                .then(Commands.literal("reload").executes(ctx -> {
                    plugin.reloadConfig();
                    settings.reload(plugin.getConfig());
                    Msg.ok(ctx.getSource().getSender(), "Command-control config reloaded.");
                    return 1;
                }))
                .build(), "Per-command cooldowns and costs"));
    }
}
