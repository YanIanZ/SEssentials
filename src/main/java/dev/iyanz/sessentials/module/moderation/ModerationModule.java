package dev.iyanz.sessentials.module.moderation;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.store.YamlStore;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Moderation commands: kick, ban/tempban/unban, mute/unmute (chat-suppressing), kill,
 * vanish, invsee and endersee. Mutes persist to the {@code moderation} store; vanish
 * is session state. Folia-safe: actions on a target run on the target's region thread.
 */
@SuppressWarnings({"UnstableApiUsage", "deprecation"})
public final class ModerationModule implements EssModule, Listener {

    private static final String SOURCE = "SEssentials";

    /** Map sentinel for a permanent mute (never expires). The store persists {@code 0}. */
    private static final long PERMANENT = Long.MAX_VALUE;

    private SEssentialsPlugin plugin;
    private YamlStore store;
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();

    /**
     * Source of truth for the async chat mute check: player UUID → expiry epoch-millis,
     * or {@link #PERMANENT} for a permanent mute. Hydrated from the store on enable and
     * kept in sync by {@code /mute} and {@code /unmute}. The async chat listener reads
     * ONLY this map so it never touches the non-thread-safe {@link YamlStore} config.
     */
    private final Map<UUID, Long> mutes = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "moderation";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.plugin = plugin;
        this.store = plugin.stores().get("moderation");
        hydrateMutes();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getPluginManager().registerEvents(new InvseeListener(), plugin);

        plugin.commands(reg -> {
            reg.register(Commands.literal("kick")
                    .requires(s -> s.getSender().hasPermission("sessentials.kick"))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .executes(ctx -> kick(ctx, null))
                            .then(Commands.argument("reason", StringArgumentType.greedyString())
                                    .executes(ctx -> kick(ctx, StringArgumentType.getString(ctx, "reason")))))
                    .build(), "Kick a player");

            reg.register(Commands.literal("ban")
                    .requires(s -> s.getSender().hasPermission("sessentials.ban"))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .executes(ctx -> ban(ctx, null, null))
                            .then(Commands.argument("reason", StringArgumentType.greedyString())
                                    .executes(ctx -> ban(ctx, null, StringArgumentType.getString(ctx, "reason")))))
                    .build(), "Ban a player");

            reg.register(Commands.literal("tempban")
                    .requires(s -> s.getSender().hasPermission("sessentials.tempban"))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .then(Commands.argument("duration", StringArgumentType.word())
                                    .executes(ctx -> ban(ctx, StringArgumentType.getString(ctx, "duration"), null))
                                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                                            .executes(ctx -> ban(ctx, StringArgumentType.getString(ctx, "duration"),
                                                    StringArgumentType.getString(ctx, "reason"))))))
                    .build(), "Temporarily ban a player");

            reg.register(Commands.literal("unban")
                    .requires(s -> s.getSender().hasPermission("sessentials.unban"))
                    .then(Commands.argument("player", StringArgumentType.word())
                            .executes(this::unban))
                    .build(), "Unban a player", java.util.List.of("pardon"));

            reg.register(Commands.literal("mute")
                    .requires(s -> s.getSender().hasPermission("sessentials.mute"))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .executes(ctx -> mute(ctx, null))
                            .then(Commands.argument("duration", StringArgumentType.word())
                                    .executes(ctx -> mute(ctx, StringArgumentType.getString(ctx, "duration")))))
                    .build(), "Mute a player");

            reg.register(Commands.literal("unmute")
                    .requires(s -> s.getSender().hasPermission("sessentials.mute"))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .executes(this::unmute))
                    .build(), "Unmute a player");

            reg.register(Commands.literal("kill")
                    .requires(s -> s.getSender().hasPermission("sessentials.kill"))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .executes(this::kill))
                    .build(), "Kill a player");

            reg.register(Commands.literal("vanish")
                    .requires(s -> s.getSender().hasPermission("sessentials.vanish"))
                    .executes(Cmds.playerExec(this::toggleVanish))
                    .build(), "Toggle vanish", java.util.List.of("v"));

            reg.register(Commands.literal("invsee")
                    .requires(s -> s.getSender().hasPermission("sessentials.invsee"))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .executes(this::invsee))
                    .build(), "View + edit a player's inventory");

            reg.register(Commands.literal("endersee")
                    .requires(s -> s.getSender().hasPermission("sessentials.endersee"))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .executes(ctx -> openInv(ctx, true)))
                    .build(), "View a player's ender chest");
        });
    }

    // --- commands ----------------------------------------------------------

    private Player online(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "player");
        Player p = Bukkit.getPlayerExact(name);
        if (p == null) {
            Msg.err(ctx.getSource().getSender(), name + " is not online.");
        }
        return p;
    }

    private int kick(CommandContext<CommandSourceStack> ctx, String reason) {
        Player t = online(ctx);
        if (t == null) {
            return 0;
        }
        String msg = reason != null ? reason : "Kicked by an operator.";
        Schedulers.entity(plugin, t, () -> t.kick(Component.text(msg)));
        Msg.ok(ctx.getSource().getSender(), "Kicked " + t.getName() + ".");
        return 1;
    }

    private int ban(CommandContext<CommandSourceStack> ctx, String duration, String reason) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        Date expires = null;
        if (duration != null) {
            Duration d = Durations.parse(duration);
            if (d == null) {
                Msg.err(sender, "Invalid duration: " + duration);
                return 0;
            }
            expires = Date.from(Instant.now().plus(d));
        }
        String msg = reason != null ? reason : "Banned by an operator.";
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            Date exp = expires;
            Schedulers.entity(plugin, online, () -> online.ban(msg, exp, SOURCE, true));
        } else {
            Bukkit.getBanList(BanList.Type.NAME).addBan(name, msg, expires, SOURCE);
        }
        Msg.ok(sender, (duration != null ? "Temp-banned " : "Banned ") + name + ".");
        return 1;
    }

    private int unban(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "player");
        Bukkit.getBanList(BanList.Type.NAME).pardon(name);
        Msg.ok(ctx.getSource().getSender(), "Unbanned " + name + ".");
        return 1;
    }

    private int mute(CommandContext<CommandSourceStack> ctx, String duration) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        long until = 0L;
        if (duration != null) {
            Duration d = Durations.parse(duration);
            if (d == null) {
                Msg.err(sender, "Invalid duration: " + duration);
                return 0;
            }
            until = Instant.now().plus(d).toEpochMilli();
        }
        store.set("mutes." + target.getUniqueId(), until);
        store.save();
        mutes.put(target.getUniqueId(), until == 0L ? PERMANENT : until);
        Msg.ok(sender, "Muted " + name + (duration != null ? " for " + duration : " permanently") + ".");
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            Msg.err(online, "You have been muted.");
        }
        return 1;
    }

    private int unmute(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "player");
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        store.remove("mutes." + target.getUniqueId());
        store.save();
        mutes.remove(target.getUniqueId());
        Msg.ok(ctx.getSource().getSender(), "Unmuted " + name + ".");
        return 1;
    }

    private int kill(CommandContext<CommandSourceStack> ctx) {
        Player t = online(ctx);
        if (t == null) {
            return 0;
        }
        Schedulers.entity(plugin, t, () -> t.setHealth(0.0));
        Msg.ok(ctx.getSource().getSender(), "Killed " + t.getName() + ".");
        return 1;
    }

    private void toggleVanish(Player p) {
        if (vanished.remove(p.getUniqueId())) {
            for (Player other : Bukkit.getOnlinePlayers()) {
                // Visibility must be mutated on the OTHER player's region thread (Folia).
                Schedulers.entity(plugin, other, () -> other.showPlayer(plugin, p));
            }
            Msg.ok(p, "You are now visible.");
        } else {
            vanished.add(p.getUniqueId());
            for (Player other : Bukkit.getOnlinePlayers()) {
                // Visibility must be mutated on the OTHER player's region thread (Folia).
                Schedulers.entity(plugin, other, () -> {
                    if (!other.hasPermission("sessentials.vanish")) {
                        other.hidePlayer(plugin, p);
                    }
                });
            }
            Msg.ok(p, "You are now vanished.");
        }
    }

    private int invsee(CommandContext<CommandSourceStack> ctx) {
        Player viewer = Cmds.player(ctx);
        if (viewer == null) {
            return 0;
        }
        Player t = online(ctx);
        if (t == null) {
            return 0;
        }
        InvseeMenu.open(plugin, viewer, t);
        return 1;
    }

    private int openInv(CommandContext<CommandSourceStack> ctx, boolean ender) {
        Player viewer = Cmds.player(ctx);
        if (viewer == null) {
            return 0;
        }
        Player t = online(ctx);
        if (t == null) {
            return 0;
        }
        Schedulers.entity(plugin, viewer, () ->
                viewer.openInventory(ender ? t.getEnderChest() : t.getInventory()));
        return 1;
    }

    /** Loads persisted mutes from the store into {@link #mutes} once, on enable. */
    private void hydrateMutes() {
        for (String key : store.keys("mutes")) {
            try {
                UUID id = UUID.fromString(key);
                long until = store.getLong("mutes." + key, 0L);
                mutes.put(id, until == 0L ? PERMANENT : until);
            } catch (IllegalArgumentException ignored) {
                // Skip a malformed (non-UUID) key rather than fail enable.
            }
        }
    }

    // --- listeners ---------------------------------------------------------

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncChatEvent event) {
        // Async thread: read ONLY the thread-safe map — never the raw store config.
        UUID id = event.getPlayer().getUniqueId();
        Long until = mutes.get(id);
        if (until == null) {
            return; // not muted
        }
        if (until != PERMANENT && System.currentTimeMillis() > until) {
            // Expired: drop from the map here (thread-safe), and clean the store off
            // this async chat thread (the store is not safe to mutate from here).
            if (mutes.remove(id, until)) {
                Schedulers.async(plugin, t -> {
                    store.remove("mutes." + id);
                    store.save();
                });
            }
            return;
        }
        event.setCancelled(true);
        event.viewers().clear();
        Msg.err(event.getPlayer(), "You are muted.");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Prevent the session-state vanished set from leaking UUIDs of gone players.
        vanished.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joiner = event.getPlayer();
        if (joiner.hasPermission("sessentials.vanish")) {
            return;
        }
        for (UUID id : vanished) {
            Player v = Bukkit.getPlayer(id);
            if (v != null) {
                joiner.hidePlayer(plugin, v);
            }
        }
    }
}
