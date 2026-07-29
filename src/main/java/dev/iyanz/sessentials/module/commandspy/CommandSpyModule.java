package dev.iyanz.sessentials.module.commandspy;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

/**
 * A moderator "command spy" toggle: {@code /commandspy} (alias {@code /cspy}) lets a
 * staff member watch, in real time, every command other players run on the server.
 *
 * <p>The module owns the single piece of shared, in-memory state its two collaborators
 * need: the set of players who currently have command spy enabled. The set is
 * process-local and is not persisted to disk, so it resets on restart. Command
 * registration lives here; the mirroring of each command to the watchers is handled by
 * {@link CommandSpyListener}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class CommandSpyModule implements EssModule {

    /** Players currently watching the commands others run (backed by a concurrent map). */
    private final Set<UUID> spies = ConcurrentHashMap.newKeySet();

    @Override
    public String name() {
        return "commandspy";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager()
                .registerEvents(new CommandSpyListener(plugin, spies), plugin);

        plugin.commands(reg -> reg.register(Commands.literal("commandspy")
                .requires(s -> s.getSender().hasPermission("sessentials.commandspy"))
                .executes(Cmds.playerExec(this::toggle))
                .build(), "Toggle seeing commands other players run", List.of("cspy")));
    }

    /** Flips command spy for {@code player} and confirms the new state in small caps. */
    private void toggle(Player player) {
        if (spies.remove(player.getUniqueId())) {
            Msg.ok(player, "Command spy disabled.");
        } else {
            spies.add(player.getUniqueId());
            Msg.ok(player, "Command spy enabled.");
        }
    }
}
