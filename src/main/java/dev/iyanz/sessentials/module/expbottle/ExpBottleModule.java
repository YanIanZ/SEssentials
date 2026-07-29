package dev.iyanz.sessentials.module.expbottle;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownExpBottle;

/**
 * "Throw your XP" convenience command (CMI parity). {@code /expbottle <levels>} spends
 * that many of the player's experience levels and launches one thrown experience bottle
 * per level in the direction they are facing.
 *
 * <p>The number of bottles is clamped to {@link #MAX_BOTTLES} so a stray large argument
 * cannot spawn thousands of entities; when the request is clamped the player is told.
 * The command is player-only and gated behind {@code sessentials.expbottle}.</p>
 *
 * <p>Folia-safe: a normal command executor already runs on the sender's own region
 * thread, so spending levels and launching projectiles happens inline with no hop.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class ExpBottleModule implements EssModule {

    /** Permission required to throw experience bottles. */
    private static final String PERMISSION = "sessentials.expbottle";

    /** Upper bound on bottles launched in a single command, to avoid entity floods. */
    private static final int MAX_BOTTLES = 64;

    @Override
    public String name() {
        return "expbottle";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("expbottle")
                .requires(s -> s.getSender().hasPermission(PERMISSION))
                .then(Commands.argument("levels", IntegerArgumentType.integer(1))
                        .executes(ExpBottleModule::throwBottles))
                .build(), "Throw your XP as experience bottles"));
    }

    /**
     * Spends the requested experience levels and launches one thrown experience bottle
     * per level. Clamps the count to {@link #MAX_BOTTLES}, informing the player if the
     * request was reduced, and aborts (with an error) when the player lacks the levels.
     *
     * @param ctx the Brigadier command context
     * @return {@code 1} when bottles were thrown, {@code 0} otherwise
     */
    private static int throwBottles(CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }

        int levels = IntegerArgumentType.getInteger(ctx, "levels");
        if (levels > MAX_BOTTLES) {
            levels = MAX_BOTTLES;
            Msg.info(player, "Capped at " + MAX_BOTTLES + " bottles per throw.");
        }

        if (player.getLevel() < levels) {
            Msg.err(player, "You don't have " + levels + " levels.");
            return 0;
        }

        player.setLevel(player.getLevel() - levels);
        for (int i = 0; i < levels; i++) {
            player.launchProjectile(ThrownExpBottle.class);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_BOTTLE_THROW, 1.0f, 1.0f);

        Msg.ok(player, "Threw " + levels + " experience bottle" + (levels == 1 ? "" : "s") + ".");
        return 1;
    }
}
