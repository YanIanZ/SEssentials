package dev.iyanz.sessentials.module.identity;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.store.YamlStore;
import dev.iyanz.sessentials.util.LegacyColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

/**
 * Applies, persists and restores player nicknames. A nickname replaces both the
 * chat {@link Player#displayName() display name} and the {@link Player#playerListName()
 * tab-list name}; it is stored per player in the {@code "identity"} data store (key
 * {@code "<uuid>.nick"}) and re-applied automatically on join via {@link #reapply}.
 *
 * <p>Colour is only kept in the stored/applied value when the command sender held
 * {@code sessentials.nick.color} at the moment the nickname was set — otherwise any
 * MiniMessage tags are stripped down to plain text <em>before</em> storing, so a
 * later re-application (e.g. on join, or after the permission is revoked) can never
 * "regain" colour the player was never entitled to.</p>
 */
public final class NickService {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String KEY_NICK = ".nick";

    private final SEssentialsPlugin plugin;

    /**
     * @param plugin the owning plugin, used for its data store and Folia-safe schedulers
     */
    public NickService(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Sets, persists and applies {@code target}'s nickname.
     *
     * @param target       the player to nickname
     * @param rawInput     the requested nickname text (may contain MiniMessage tags)
     * @param colorAllowed whether the command sender may keep colour/formatting tags
     */
    public void setNick(Player target, String rawInput, boolean colorAllowed) {
        String effective = colorAllowed ? rawInput : toPlain(rawInput);
        store().set(target.getUniqueId() + KEY_NICK, effective);
        store().save();
        Component component = render(effective);
        Schedulers.entity(plugin, target, () -> apply(target, component));
    }

    /**
     * Clears {@code target}'s persisted nickname and restores their real name as the
     * display/tab-list name.
     *
     * @param target the player to reset
     */
    public void clearNick(Player target) {
        store().remove(target.getUniqueId() + KEY_NICK);
        store().save();
        Component component = Component.text(target.getName());
        Schedulers.entity(plugin, target, () -> apply(target, component));
    }

    /**
     * Re-applies {@code player}'s persisted nickname (or their real name if none is
     * stored). Intended to be called from a join listener.
     *
     * @param player the player who just joined
     */
    public void reapply(Player player) {
        String stored = store().getString(player.getUniqueId() + KEY_NICK);
        Component component = stored != null ? render(stored) : Component.text(player.getName());
        Schedulers.entity(plugin, player, () -> apply(player, component));
    }

    private void apply(Player player, Component name) {
        player.displayName(name);
        player.playerListName(name);
    }

    private Component render(String text) {
        return MM.deserialize(LegacyColors.toMiniMessage(text));
    }

    private String toPlain(String text) {
        return PlainTextComponentSerializer.plainText().serialize(render(text));
    }

    private YamlStore store() {
        return plugin.stores().get("identity");
    }
}
