package dev.iyanz.sessentials.module.inspect;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Registers {@code /whois}: a combined staff report about an online player — real
 * name/display name, UUID, location, gamemode, vitals, ping, IP address, first-joined
 * date and op status.
 *
 * <p>Every field read here (name, UUID, location, gamemode, health, food, ping,
 * address, first-played timestamp, op flag) is a simple, already-resolved value on
 * the {@link Player}/{@link org.bukkit.OfflinePlayer} object, so this reads them
 * directly from the calling thread rather than hopping to the target's region
 * thread.</p>
 */
@SuppressWarnings({"UnstableApiUsage", "deprecation"})
final class WhoisCommand {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH).withZone(ZoneId.systemDefault());

    private WhoisCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            var node = Commands.literal("whois")
                    .requires(s -> s.getSender().hasPermission("sessentials.whois"))
                    .then(Commands.argument("player", StringArgumentType.word())
                            .suggests(Cmds.PLAYERS)
                            .executes(WhoisCommand::report))
                    .build();
            reg.register(node, "Show combined info about an online player");
        });
    }

    private static int report(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Msg.err(sender, name + " is not online.");
            return 0;
        }

        String display = PlainTextComponentSerializer.plainText().serialize(target.displayName());
        Msg.value(sender, "Player:",
                display.equalsIgnoreCase(target.getName()) ? target.getName() : target.getName() + " (" + display + ")");
        Msg.value(sender, "UUID:", target.getUniqueId().toString());
        Msg.value(sender, "Location:", InspectFormat.location(target.getLocation()));
        Msg.value(sender, "Gamemode:", InspectFormat.titleCase(target.getGameMode().name()));
        Msg.value(sender, "Health:", InspectFormat.decimal(target.getHealth()) + " / " + InspectFormat.decimal(target.getMaxHealth()));
        Msg.value(sender, "Food:", target.getFoodLevel() + " / 20");
        Msg.value(sender, "Ping:", target.getPing() + "ms");
        Msg.value(sender, "IP:", ipOf(target));
        Msg.value(sender, "First joined:", firstJoined(target));
        Msg.value(sender, "Op:", target.isOp() ? "yes" : "no");
        return 1;
    }

    /** @return the target's connecting address as plain text, or {@code "unknown"}. */
    private static String ipOf(Player target) {
        var address = target.getAddress();
        if (address == null || address.getAddress() == null) {
            return "unknown";
        }
        return address.getAddress().getHostAddress();
    }

    /** @return the target's first-played date, or {@code "unknown"} if unset. */
    private static String firstJoined(Player target) {
        long millis = target.getFirstPlayed();
        if (millis <= 0L) {
            return "unknown";
        }
        return DATE_FORMAT.format(Instant.ofEpochMilli(millis));
    }
}
