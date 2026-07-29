package dev.iyanz.sessentials.module.damageindicator;

import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Damage indicator module: whenever a {@link org.bukkit.entity.LivingEntity} takes
 * damage, a short-lived floating number showing the damage dealt pops up slightly
 * above it and drifts upward before vanishing (see {@link DamageIndicatorListener}
 * and {@link DamageIndicatorRenderer}).
 *
 * <p>The feature is globally toggled by the {@code damage-indicator.enabled} config
 * key (default {@code true}). Individual players may additionally opt out of having
 * numbers appear above themselves via the {@code /damageindicator} command; those
 * opt-outs live in memory only ({@link DamageIndicatorOptOut}) and are cleared on
 * quit.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class DamageIndicatorModule implements EssModule {

    @Override
    public String name() {
        return "damageindicator";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        DamageIndicatorOptOut optOut = new DamageIndicatorOptOut();
        plugin.getServer().getPluginManager()
                .registerEvents(new DamageIndicatorListener(plugin, optOut), plugin);

        plugin.commands(reg -> reg.register(Commands.literal("damageindicator")
                .requires(s -> s.getSender().hasPermission("sessentials.damageindicator"))
                .executes(Cmds.playerExec(p -> {
                    boolean nowHidden = optOut.toggle(p.getUniqueId());
                    if (nowHidden) {
                        Msg.ok(p, "Damage numbers will no longer appear above you.");
                    } else {
                        Msg.ok(p, "Damage numbers will now appear above you.");
                    }
                }))
                .build(), "Toggle floating damage numbers appearing above you", List.of("dmgindicator")));
    }
}
