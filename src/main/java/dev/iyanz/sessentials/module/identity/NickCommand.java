package dev.iyanz.sessentials.module.identity;

import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Registers {@code /nick} (alias {@code /nickname}): set or clear the sender's own
 * nickname, or — with {@code sessentials.nick.others} — an online player's.
 *
 * <p>A coloured nickname (MiniMessage tags) requires {@code sessentials.nick.color}
 * on the <em>sender</em>; the resulting {@code name} argument uses Brigadier's
 * quoted-or-word string type, so a plain name needs no quoting (e.g. {@code /nick
 * Bob}) while a coloured one must be quoted (e.g. {@code /nick "<red>Bob"}) since
 * angle brackets are not valid in an unquoted Brigadier token.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class NickCommand {

    private NickCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin      the owning plugin
     * @param nickService the service used to apply/persist nicknames
     */
    public static void register(SEssentialsPlugin plugin, NickService nickService) {
        plugin.commands(reg -> {
            LiteralArgumentBuilder<CommandSourceStack> offBranch = Commands.literal("off")
                    .executes(Cmds.playerExec(p -> {
                        nickService.clearNick(p);
                        Msg.ok(p, "Nickname reset.");
                    }))
                    .then(Commands.argument("target", StringArgumentType.word())
                            .suggests(Cmds.PLAYERS)
                            .requires(s -> s.getSender().hasPermission("sessentials.nick.others"))
                            .executes(ctx -> {
                                CommandSender sender = ctx.getSource().getSender();
                                String targetName = StringArgumentType.getString(ctx, "target");
                                Player target = Bukkit.getPlayerExact(targetName);
                                if (target == null) {
                                    Msg.err(sender, targetName + " is not online.");
                                    return 0;
                                }
                                nickService.clearNick(target);
                                Msg.ok(sender, "Reset " + target.getName() + "'s nickname.");
                                return 1;
                            }));

            RequiredArgumentBuilder<CommandSourceStack, String> nameBranch =
                    Commands.argument("name", StringArgumentType.string())
                            .executes(ctx -> {
                                Player p = Cmds.player(ctx);
                                if (p == null) {
                                    return 0;
                                }
                                String raw = StringArgumentType.getString(ctx, "name");
                                boolean colorAllowed = p.hasPermission("sessentials.nick.color");
                                nickService.setNick(p, raw, colorAllowed);
                                Msg.ok(p, "Nickname set.");
                                return 1;
                            })
                            .then(Commands.argument("target", StringArgumentType.word())
                                    .suggests(Cmds.PLAYERS)
                                    .requires(s -> s.getSender().hasPermission("sessentials.nick.others"))
                                    .executes(ctx -> {
                                        CommandSender sender = ctx.getSource().getSender();
                                        String raw = StringArgumentType.getString(ctx, "name");
                                        String targetName = StringArgumentType.getString(ctx, "target");
                                        Player target = Bukkit.getPlayerExact(targetName);
                                        if (target == null) {
                                            Msg.err(sender, targetName + " is not online.");
                                            return 0;
                                        }
                                        boolean colorAllowed = sender.hasPermission("sessentials.nick.color");
                                        nickService.setNick(target, raw, colorAllowed);
                                        Msg.ok(sender, "Set " + target.getName() + "'s nickname.");
                                        return 1;
                                    }));

            var node = Commands.literal("nick")
                    .requires(s -> s.getSender().hasPermission("sessentials.nick"))
                    .then(offBranch)
                    .then(nameBranch)
                    .build();
            reg.register(node, "Set your nickname, or another player's", List.of("nickname"));
        });
    }
}
