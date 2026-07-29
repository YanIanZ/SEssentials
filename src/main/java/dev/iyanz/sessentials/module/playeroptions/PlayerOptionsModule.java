package dev.iyanz.sessentials.module.playeroptions;

import java.util.UUID;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.store.YamlStore;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

/**
 * Per-player preference toggles: whether a player receives private messages
 * ({@code /msgtoggle}), accepts incoming teleport requests ({@code /tptoggle}), and
 * whether flight should be restored automatically when they rejoin ({@code /flytoggle}).
 *
 * <p>Every toggle is persisted in the {@code "playeroptions"} data store under
 * {@code "<uuid>.<option>"} boolean keys, so preferences survive restarts.
 * {@link #msgEnabled} and {@link #tpEnabled} are exposed as a public API so other
 * modules (private messaging, teleport requests) could consult a player's preference
 * before acting on them; this module does not itself gate anything outside its own
 * commands and the flight-on-join listener registered below.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class PlayerOptionsModule implements EssModule {

    private static final String STORE_NAME = "playeroptions";
    private static final String OPTION_MSG = "msgtoggle";
    private static final String OPTION_TP = "tptoggle";
    private static final String OPTION_FLY = "flytoggle";

    @Override
    public String name() {
        return "playeroptions";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            reg.register(Commands.literal("msgtoggle")
                    .requires(s -> s.getSender().hasPermission("sessentials.msgtoggle"))
                    .executes(Cmds.playerExec(p -> toggle(plugin, p, OPTION_MSG, true,
                            "You will now receive private messages.",
                            "You will no longer receive private messages.")))
                    .build(), "Toggle whether you receive private messages");

            reg.register(Commands.literal("tptoggle")
                    .requires(s -> s.getSender().hasPermission("sessentials.tptoggle"))
                    .executes(Cmds.playerExec(p -> toggle(plugin, p, OPTION_TP, true,
                            "You will now accept teleport requests.",
                            "You will no longer accept teleport requests.")))
                    .build(), "Toggle whether you accept teleport requests");

            reg.register(Commands.literal("flytoggle")
                    .requires(s -> s.getSender().hasPermission("sessentials.flytoggle"))
                    .executes(Cmds.playerExec(p -> toggle(plugin, p, OPTION_FLY, false,
                            "Flight will now be kept on when you rejoin.",
                            "Flight will no longer be kept on when you rejoin.")))
                    .build(), "Toggle keeping flight enabled across rejoins");
        });

        plugin.getServer().getPluginManager().registerEvents(new PlayerOptionsListener(plugin), plugin);
    }

    /** Flips {@code option} for {@code player}, persists it, and messages the new state. */
    private static void toggle(SEssentialsPlugin plugin, Player player, String option, boolean defaultValue,
            String enabledText, String disabledText) {
        boolean enabled = !get(plugin, player.getUniqueId(), option, defaultValue);
        set(plugin, player.getUniqueId(), option, enabled);
        if (enabled) {
            Msg.ok(player, enabledText);
        } else {
            Msg.info(player, disabledText);
        }
    }

    private static boolean get(SEssentialsPlugin plugin, UUID uuid, String option, boolean defaultValue) {
        YamlStore store = plugin.stores().get(STORE_NAME);
        return store.config().getBoolean(uuid + "." + option, defaultValue);
    }

    private static void set(SEssentialsPlugin plugin, UUID uuid, String option, boolean value) {
        YamlStore store = plugin.stores().get(STORE_NAME);
        store.set(uuid + "." + option, value);
        store.save();
    }

    /**
     * Whether {@code uuid} currently accepts private messages (default {@code true}).
     * No other module calls this yet; it is exposed so the private-messaging module
     * could gate delivery on this preference in the future.
     *
     * @param plugin the owning plugin, used to reach the data store
     * @param uuid   the player being checked
     * @return {@code true} if the player accepts private messages
     */
    public static boolean msgEnabled(SEssentialsPlugin plugin, UUID uuid) {
        return get(plugin, uuid, OPTION_MSG, true);
    }

    /**
     * Whether {@code uuid} currently accepts teleport requests (default {@code true}).
     * No other module calls this yet; it is exposed so the teleport-request commands
     * could gate sending a request on this preference in the future.
     *
     * @param plugin the owning plugin, used to reach the data store
     * @param uuid   the player being checked
     * @return {@code true} if the player accepts teleport requests
     */
    public static boolean tpEnabled(SEssentialsPlugin plugin, UUID uuid) {
        return get(plugin, uuid, OPTION_TP, true);
    }

    /**
     * Whether {@code uuid} wants flight restored automatically when they join.
     * Consulted by {@link PlayerOptionsListener}.
     *
     * @param plugin the owning plugin, used to reach the data store
     * @param uuid   the player being checked
     * @return {@code true} if flight should be restored on join (default {@code false})
     */
    static boolean flyKeepEnabled(SEssentialsPlugin plugin, UUID uuid) {
        return get(plugin, uuid, OPTION_FLY, false);
    }
}
