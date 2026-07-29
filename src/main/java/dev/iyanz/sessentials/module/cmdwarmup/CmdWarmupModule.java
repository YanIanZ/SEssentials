package dev.iyanz.sessentials.module.cmdwarmup;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;

/**
 * A movement-cancellable warmup delay before configured commands run, driven by the
 * {@code command-warmup} config section (see {@link CmdWarmupSettings}) and enforced by
 * {@link CmdWarmupListener}. {@code /cmdwarmup reload} re-reads the config at runtime.
 */
@SuppressWarnings("UnstableApiUsage")
public final class CmdWarmupModule implements EssModule {

    private CmdWarmupSettings settings;

    @Override
    public String name() {
        return "cmdwarmup";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.settings = CmdWarmupSettings.load(plugin.getConfig());
        plugin.getServer().getPluginManager().registerEvents(new CmdWarmupListener(plugin, settings), plugin);
        plugin.commands(reg -> reg.register(Commands.literal("cmdwarmup")
                .requires(s -> s.getSender().hasPermission("sessentials.cmdwarmup"))
                .then(Commands.literal("reload").executes(ctx -> {
                    plugin.reloadConfig();
                    settings.reload(plugin.getConfig());
                    Msg.ok(ctx.getSource().getSender(), "Command-warmup config reloaded.");
                    return 1;
                }))
                .build(), "Movement-cancellable command warmups"));
    }
}
