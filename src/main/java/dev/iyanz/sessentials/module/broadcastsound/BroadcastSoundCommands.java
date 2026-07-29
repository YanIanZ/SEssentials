package dev.iyanz.sessentials.module.broadcastsound;

import java.util.Locale;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
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
 * Registers and handles {@code /bsound <sound> [volume] [pitch]} — an operator command
 * that plays a Minecraft sound to every online player at their own location.
 *
 * <p>The {@code sound} argument is resolved against Paper's {@link Registry#SOUNDS}
 * registry (on 1.21.9 {@link Sound} is a registry interface, not an enum), so any
 * vanilla sound key such as {@code entity.player.levelup} works; unknown keys are
 * rejected with an error. Optional {@code volume} and {@code pitch} floats default to
 * {@code 1.0} each and are constrained to be non-negative. Playback for every recipient
 * is hopped onto that player's own region thread, keeping the command Folia-safe.</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class BroadcastSoundCommands {

    /** Operator permission required to broadcast a sound. */
    private static final String PERMISSION = "sessentials.bsound";

    /** Default volume/pitch when the caller omits them. */
    private static final float DEFAULT_LEVEL = 1.0f;

    private BroadcastSoundCommands() {
    }

    /** Registers {@code /bsound <sound> [volume] [pitch]}. */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("bsound")
                .requires(s -> s.getSender().hasPermission(PERMISSION))
                .then(Commands.argument("sound", StringArgumentType.word())
                        .suggests(SoundSuggestions.of())
                        .executes(ctx -> play(plugin, ctx, DEFAULT_LEVEL, DEFAULT_LEVEL))
                        .then(Commands.argument("volume", FloatArgumentType.floatArg(0.0f))
                                .executes(ctx -> play(plugin, ctx,
                                        FloatArgumentType.getFloat(ctx, "volume"), DEFAULT_LEVEL))
                                .then(Commands.argument("pitch", FloatArgumentType.floatArg(0.0f))
                                        .executes(ctx -> play(plugin, ctx,
                                                FloatArgumentType.getFloat(ctx, "volume"),
                                                FloatArgumentType.getFloat(ctx, "pitch"))))))
                .build(), "Play a sound to every online player"));
    }

    /**
     * Resolves the requested sound key and plays it to every online player at their own
     * location, each on its own region thread (Folia-safe).
     *
     * @param plugin the owning plugin (for scheduler access)
     * @param ctx    the command context (carries the {@code sound} argument)
     * @param volume the playback volume
     * @param pitch  the playback pitch
     * @return 1 if the sound was broadcast, 0 if the key was unknown
     */
    private static int play(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx,
                            float volume, float pitch) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "sound");
        Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
        if (sound == null) {
            Msg.err(sender, "Unknown sound: " + name);
            return 0;
        }
        int recipients = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            Schedulers.entity(plugin, player, () -> player.playSound(player.getLocation(), sound, volume, pitch));
            recipients++;
        }
        Msg.ok(sender, "Played " + name + " to " + recipients + " players.");
        return 1;
    }
}
