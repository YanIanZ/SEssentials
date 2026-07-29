package dev.iyanz.sessentials.module.autobroadcast;

import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Periodically broadcasts a rotating list of announcements to every online player
 * and the console.
 *
 * <p>Reads {@code autobroadcast.enabled} (default {@code true}),
 * {@code autobroadcast.interval-seconds} (default {@code 300}) and
 * {@code autobroadcast.messages} (a MiniMessage string list; a small built-in default
 * is used if that list is empty) once at {@link #enable}. A single repeating async
 * task ({@link Schedulers#asyncRepeating}) walks the list in order, wrapping back to
 * the start; {@code /broadcastnext} lets an operator force the next message
 * immediately, independent of the schedule.</p>
 *
 * <p>Folia-safe: the rotation task itself never touches entity/world state directly,
 * it only sends messages, hopping to each recipient's own region thread via
 * {@link Schedulers#entity}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class AutoBroadcastModule implements EssModule {

    /** Ticks per second, used to convert the configured interval to Bukkit ticks. */
    private static final long TICKS_PER_SECOND = 20L;

    /** Used when {@code autobroadcast.messages} is unset or empty. */
    private static final List<String> DEFAULT_MESSAGES = List.of(
            "<gray>Type <#FFD54F>/rules<gray> to read the server rules.",
            "<gray>Need a hand? Ask a staff member for help.",
            "<gray>Enjoying SourbyCraft? Tell your friends about us!");

    private SEssentialsPlugin plugin;
    private List<String> messages = DEFAULT_MESSAGES;
    private int cursor;
    private ScheduledTask task;

    @Override
    public String name() {
        return "autobroadcast";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.plugin = plugin;
        this.messages = loadMessages(plugin);

        plugin.commands(reg -> reg.register(Commands.literal("broadcastnext")
                .requires(s -> s.getSender().hasPermission("sessentials.autobroadcast"))
                .executes(ctx -> {
                    broadcastNext();
                    Msg.ok(ctx.getSource().getSender(), "Sent the next broadcast.");
                    return 1;
                })
                .build(), "Force the next scheduled broadcast"));

        if (!plugin.getConfig().getBoolean("autobroadcast.enabled", true)) {
            return;
        }
        long periodTicks = plugin.getConfig().getLong("autobroadcast.interval-seconds", 300L) * TICKS_PER_SECOND;
        this.task = Schedulers.asyncRepeating(plugin, this::broadcastNext, periodTicks, periodTicks);
    }

    @Override
    public void disable(SEssentialsPlugin plugin) {
        if (task != null) {
            task.cancel();
        }
    }

    /** Sends the next message in rotation to every online player and the console, then advances the cursor. */
    private synchronized void broadcastNext() {
        if (messages.isEmpty()) {
            return;
        }
        String next = messages.get(cursor);
        cursor = (cursor + 1) % messages.size();

        Component message = Msg.mm(next);
        Bukkit.getConsoleSender().sendMessage(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            Schedulers.entity(plugin, player, () -> player.sendMessage(message));
        }
    }

    /** @return the configured broadcast messages, or {@link #DEFAULT_MESSAGES} if none are set. */
    private static List<String> loadMessages(SEssentialsPlugin plugin) {
        List<String> configured = plugin.getConfig().getStringList("autobroadcast.messages");
        return configured.isEmpty() ? DEFAULT_MESSAGES : configured;
    }
}
