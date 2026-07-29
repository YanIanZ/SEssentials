package dev.iyanz.sessentials.module.vitals;

import java.util.List;
import java.util.function.Consumer;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Extra vital-stat commands not covered by {@code module.playerstate}:
 * {@code /extinguish} (alias {@code /ext}) and {@code /suicide}.
 *
 * <p>{@code /heal}, {@code /feed} and {@code /god} deliberately live in
 * {@code module.playerstate.VitalityCommands} (which also owns {@code /fly} and
 * {@code /speed}); this module only adds the two commands that module lacks, to avoid
 * double-registering the same literals.</p>
 *
 * <p>{@code /extinguish} acts on the sender by default, or on an optional {@code [player]}
 * target guarded by {@code sessentials.extinguish.others}; {@code /suicide} is self-only.</p>
 *
 * <p>Folia-safe: every entity mutation is dispatched to the target's own region thread via
 * {@link Schedulers#entity}. Messages to the acting sender are sent inline (the command
 * executor already runs on the sender's region thread); messages to a different target are
 * sent from within that target's region hop.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class VitalsModule implements EssModule {

    private SEssentialsPlugin plugin;

    @Override
    public String name() {
        return "vitals";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.plugin = plugin;
        plugin.commands(reg -> {
            registerVital(reg, "extinguish", "Put out fire on yourself or a player", List.of("ext"),
                    VitalsModule::extinguish, "You are no longer on fire.", "Extinguished ");
            registerSuicide(reg);
        });
    }

    // --- generic self-or-target vital commands -----------------------------------

    /**
     * Registers a "self or optional target" vital command whose only effect is a single
     * entity mutation plus a confirmation message to each party.
     *
     * @param reg           the command registrar
     * @param command       the command literal (also the permission stem)
     * @param description   the help description
     * @param aliases       extra literals to register the same node under
     * @param mutation      the entity mutation to apply on the target's region thread
     * @param targetMessage the confirmation shown to the affected player
     * @param actorVerb     the sender-facing verb prefix (e.g. {@code "Extinguished "}),
     *                      followed by the target's name; only shown when acting on someone else
     */
    private void registerVital(Commands reg, String command, String description, List<String> aliases,
                               Consumer<Player> mutation, String targetMessage, String actorVerb) {
        LiteralCommandNode<CommandSourceStack> node = Commands.literal(command)
                .requires(s -> s.getSender().hasPermission("sessentials." + command))
                .executes(Cmds.playerExec(self -> apply(self, self, mutation, targetMessage, actorVerb)))
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests(Cmds.PLAYERS)
                        .requires(s -> s.getSender().hasPermission("sessentials." + command + ".others"))
                        .executes(ctx -> applyToNamed(ctx, mutation, targetMessage, actorVerb)))
                .build();
        reg.register(node, description, aliases);
    }

    /** Resolves the named online target for a {@code .others} invocation and applies the mutation. */
    private int applyToNamed(CommandContext<CommandSourceStack> ctx, Consumer<Player> mutation,
                             String targetMessage, String actorVerb) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "target");
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Msg.err(sender, name + " is not online.");
            return 0;
        }
        apply(sender, target, mutation, targetMessage, actorVerb);
        return 1;
    }

    /**
     * Runs {@code mutation} on {@code target}'s region thread (messaging the target there),
     * and notifies {@code actor} inline when it differs from the target.
     */
    private void apply(CommandSender actor, Player target, Consumer<Player> mutation,
                       String targetMessage, String actorVerb) {
        Schedulers.entity(plugin, target, () -> {
            mutation.accept(target);
            Msg.ok(target, targetMessage);
        });
        if (!target.equals(actor)) {
            Msg.ok(actor, actorVerb + target.getName() + ".");
        }
    }

    /** Puts out any fire currently burning a player. */
    private static void extinguish(Player target) {
        target.setFireTicks(0);
    }

    // --- /suicide ----------------------------------------------------------------

    /** Registers the self-only {@code /suicide} command. */
    private void registerSuicide(Commands reg) {
        reg.register(Commands.literal("suicide")
                .requires(s -> s.getSender().hasPermission("sessentials.suicide"))
                .executes(Cmds.playerExec(player -> {
                    Schedulers.entity(plugin, player, () -> player.setHealth(0));
                    Msg.info(player, "You have taken your own life.");
                }))
                .build(), "Take your own life");
    }
}
