package dev.iyanz.sessentials.module.hpbar;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;

/**
 * HP bar module: whenever a player damages a {@link org.bukkit.entity.LivingEntity},
 * a compact health readout for that entity is flashed in the attacker's action bar
 * (see {@link HpBarListener}).
 *
 * <p>The feature is globally toggled by the {@code hpbar.enabled} config key (default
 * {@code true}) and individually by the {@code /hpbar} command, which lets a player
 * opt out of seeing the readout on their own screen; opt-outs are persisted via
 * {@link HpBarOptOut}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class HpBarModule implements EssModule {

    @Override
    public String name() {
        return "hpbar";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new HpBarListener(plugin), plugin);

        plugin.commands(reg -> reg.register(Commands.literal("hpbar")
                .requires(s -> s.getSender().hasPermission("sessentials.hpbar"))
                .executes(Cmds.playerExec(p -> {
                    boolean nowDisabled = HpBarOptOut.toggle(plugin, p.getUniqueId());
                    if (nowDisabled) {
                        Msg.ok(p, "You will no longer see target health in your action bar.");
                    } else {
                        Msg.ok(p, "You will now see target health in your action bar.");
                    }
                }))
                .build(), "Toggle showing target health in your action bar when you attack"));
    }
}
