package dev.iyanz.sessentials.module.combat;

import java.util.List;
import java.util.UUID;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Combat module: staff freezing ({@code /freeze}, alias {@code /cuff}) and passive PvP
 * combat tagging (see {@link CombatTagListener}).
 *
 * <p>Frozen state ({@link FrozenPlayers}) and combat-tag state ({@link CombatTag}) are
 * both purely in-memory (a restart clears them), as neither needs to survive a
 * restart. A repeating async task periodically sweeps expired combat tags so a
 * "left combat" message can be sent the moment a tag lapses, even if the player takes
 * no further action to trigger a reactive check.</p>
 *
 * <p>Folia-safe: freezing/tagging themselves are plain concurrent-collection updates
 * (safe from any thread); every message sent to a player other than the command
 * sender hops to that player's own region thread via {@link Schedulers#entity}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class CombatModule implements EssModule {

    /** How often (in ticks) the combat-tag expiry sweep runs. */
    private static final long SWEEP_PERIOD_TICKS = 20L;

    private final FrozenPlayers frozenPlayers = new FrozenPlayers();
    private final CombatTag combatTag = new CombatTag();

    private SEssentialsPlugin plugin;
    private ScheduledTask sweepTask;

    @Override
    public String name() {
        return "combat";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.plugin = plugin;

        plugin.getServer().getPluginManager().registerEvents(new FreezeMoveListener(frozenPlayers), plugin);
        plugin.getServer().getPluginManager().registerEvents(new CombatTagListener(plugin, combatTag, frozenPlayers), plugin);

        this.sweepTask = Schedulers.asyncRepeating(plugin, this::sweepExpiredTags, SWEEP_PERIOD_TICKS, SWEEP_PERIOD_TICKS);

        plugin.commands(reg -> reg.register(Commands.literal("freeze")
                .requires(s -> s.getSender().hasPermission("sessentials.freeze"))
                .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                        .executes(this::freeze))
                .build(), "Toggle freezing a player in place", List.of("cuff")));
    }

    @Override
    public void disable(SEssentialsPlugin plugin) {
        if (sweepTask != null) {
            sweepTask.cancel();
        }
    }

    // --- commands ------------------------------------------------------------

    /** Toggles freezing the named online player, messaging both the sender and the target. */
    private int freeze(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Msg.err(sender, name + " is not online.");
            return 0;
        }

        boolean nowFrozen = frozenPlayers.toggle(target.getUniqueId());
        Msg.ok(sender, (nowFrozen ? "Froze " : "Unfroze ") + target.getName() + ".");
        Schedulers.entity(plugin, target, () -> {
            if (nowFrozen) {
                Msg.err(target, "You have been frozen by staff. Do not disconnect.");
            } else {
                Msg.ok(target, "You have been unfrozen.");
            }
        });
        return 1;
    }

    // --- combat-tag expiry sweep -----------------------------------------------

    /** Runs on the async scheduler: clears elapsed tags and notifies whoever is still online. */
    private void sweepExpiredTags() {
        for (UUID playerId : combatTag.sweepExpired()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                Schedulers.entity(plugin, player, () -> Msg.info(player, "You are no longer in combat."));
            }
        }
    }
}
