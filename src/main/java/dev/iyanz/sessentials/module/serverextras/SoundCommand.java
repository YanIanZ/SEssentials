package dev.iyanz.sessentials.module.serverextras;

import java.util.Locale;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Registers {@code /sound <sound> [player] [volume] [pitch]}: plays a Minecraft sound
 * to a single player at their own location — the sender when no target is named,
 * distinct from the server-wide broadcast {@code /bsound}.
 *
 * <p>The {@code sound} argument is resolved against Paper's {@link Registry#SOUNDS}
 * registry (on 1.21.9 {@link Sound} is a registry interface, not an enum), so any
 * vanilla key such as {@code entity.player.levelup} works; unknown keys are rejected.
 * Optional {@code volume} and {@code pitch} default to {@code 1.0} and must be
 * non-negative. Playback is hopped onto the recipient's own region thread, keeping
 * the command Folia-safe.</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class SoundCommand {

    /** Permission to play a sound to yourself; {@code .others} extends it to targets. */
    private static final String PERMISSION = "sessentials.sound";

    /** Default volume/pitch when the caller omits them. */
    private static final float DEFAULT_LEVEL = 1.0f;

    private SoundCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("sound")
                .requires(s -> s.getSender().hasPermission(PERMISSION))
                .then(Commands.argument("sound", StringArgumentType.word())
                        .suggests(ExtrasSuggestions.SOUND_KEYS)
                        .executes(ctx -> playToSelf(plugin, ctx))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(Cmds.PLAYERS)
                                .requires(s -> s.getSender().hasPermission(PERMISSION + ".others"))
                                .executes(ctx -> playToTarget(plugin, ctx, DEFAULT_LEVEL, DEFAULT_LEVEL))
                                .then(Commands.argument("volume", FloatArgumentType.floatArg(0.0f))
                                        .executes(ctx -> playToTarget(plugin, ctx,
                                                FloatArgumentType.getFloat(ctx, "volume"), DEFAULT_LEVEL))
                                        .then(Commands.argument("pitch", FloatArgumentType.floatArg(0.0f))
                                                .executes(ctx -> playToTarget(plugin, ctx,
                                                        FloatArgumentType.getFloat(ctx, "volume"),
                                                        FloatArgumentType.getFloat(ctx, "pitch")))))))
                .build(), "Play a sound to one player"));
    }

    /** Plays the sound to the sender themselves (player-only path). */
    private static int playToSelf(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        Player self = Cmds.player(ctx);
        if (self == null) {
            return 0;
        }
        return play(plugin, ctx, self, DEFAULT_LEVEL, DEFAULT_LEVEL);
    }

    /** Plays the sound to the named online player. */
    private static int playToTarget(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx,
                                    float volume, float pitch) {
        String name = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Msg.err(ctx.getSource().getSender(), name + " is not online.");
            return 0;
        }
        return play(plugin, ctx, target, volume, pitch);
    }

    /**
     * Resolves the sound key and plays it at the recipient's location on their own
     * region thread.
     *
     * @param plugin    the owning plugin (for scheduler access)
     * @param ctx       the command context (carries the {@code sound} argument)
     * @param recipient the player who hears the sound
     * @param volume    the playback volume
     * @param pitch     the playback pitch
     * @return 1 if the sound was played, 0 if the key was unknown
     */
    private static int play(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx,
                            Player recipient, float volume, float pitch) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "sound");
        // NamespacedKey.fromString returns null (never throws) on illegal input — word()
        // allows chars like '+' that NamespacedKey.minecraft() would reject with an
        // IllegalArgumentException, surfacing a raw framework error instead of "Unknown sound".
        NamespacedKey key = NamespacedKey.fromString(name.toLowerCase(Locale.ROOT));
        Sound sound = key == null ? null : Registry.SOUNDS.get(key);
        if (sound == null) {
            Msg.err(sender, "Unknown sound: " + name);
            return 0;
        }
        Schedulers.entity(plugin, recipient, () ->
                recipient.playSound(recipient.getLocation(), sound, volume, pitch));
        if (!recipient.equals(sender)) {
            Msg.ok(sender, "Played " + name + " to " + recipient.getName() + ".");
        }
        return 1;
    }
}
