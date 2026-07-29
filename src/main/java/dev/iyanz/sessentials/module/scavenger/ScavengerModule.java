package dev.iyanz.sessentials.module.scavenger;

import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

/**
 * The {@code /scavenger} (alias {@code /keepinv}) toggle: while enabled for a
 * player, their inventory and experience survive death instead of dropping.
 *
 * <p>Enabled state is persisted per-player (see {@link ScavengerService}) and
 * enforced by {@link ScavengerDeathListener}, which consults that registry on
 * every {@link org.bukkit.event.entity.PlayerDeathEvent}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class ScavengerModule implements EssModule {

    private ScavengerService service;

    @Override
    public String name() {
        return "scavenger";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.service = new ScavengerService(plugin.stores().get("scavenger"));

        plugin.getServer().getPluginManager().registerEvents(new ScavengerDeathListener(service), plugin);

        plugin.commands(reg -> reg.register(Commands.literal("scavenger")
                .requires(s -> s.getSender().hasPermission("sessentials.scavenger"))
                .executes(Cmds.playerExec(this::toggle))
                .build(), "Toggle keeping your inventory on death", List.of("keepinv")));
    }

    /** Toggles scavenger mode for {@code player} and reports the new state. */
    private void toggle(Player player) {
        boolean enabled = service.toggle(player.getUniqueId());
        if (enabled) {
            Msg.ok(player, "Scavenger mode enabled: your inventory will survive death.");
        } else {
            Msg.info(player, "Scavenger mode disabled.");
        }
    }
}
