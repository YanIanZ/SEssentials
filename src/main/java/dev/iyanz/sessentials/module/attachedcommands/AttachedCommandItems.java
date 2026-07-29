package dev.iyanz.sessentials.module.attachedcommands;

import java.util.ArrayList;
import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.util.SmallCaps;
import dev.iyanz.sessentials.util.Style;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Reads and writes the {@code sessentials:attached_command} tag on an item: the
 * command string (no leading {@code /}) stored in a {@link PersistentDataContainer}
 * entry, kept in sync with a single "Right-click: /command" lore line.
 *
 * <p>The command text is appended to the lore line as a literal, unparsed
 * {@link Component} rather than through MiniMessage, so an operator-chosen command
 * can never inject formatting or click events into the item's tooltip.</p>
 */
final class AttachedCommandItems {

    private static final String KEY_NAME = "attached_command";
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private AttachedCommandItems() {
    }

    /** @return the {@code sessentials:attached_command} key, scoped to {@code plugin}. */
    private static NamespacedKey key(SEssentialsPlugin plugin) {
        return new NamespacedKey(plugin, KEY_NAME);
    }

    /**
     * @param plugin the owning plugin (for the PDC key's namespace)
     * @param item   the item to inspect (may be {@code null})
     * @return the item's attached command, or {@code null} if none is tagged
     */
    static String readCommand(SEssentialsPlugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey key = key(plugin);
        return pdc.has(key, PersistentDataType.STRING) ? pdc.get(key, PersistentDataType.STRING) : null;
    }

    /**
     * Tags {@code item} with {@code command}, replacing any previously attached
     * command and its lore line.
     *
     * @param plugin  the owning plugin (for the PDC key's namespace)
     * @param item    the item to tag (its meta is replaced in place)
     * @param command the command to run on right-click (no leading {@code /})
     */
    static void attach(SEssentialsPlugin plugin, ItemStack item, String command) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey key = key(plugin);

        List<Component> lore = mutableLore(meta);
        String previous = pdc.has(key, PersistentDataType.STRING) ? pdc.get(key, PersistentDataType.STRING) : null;
        if (previous != null) {
            lore.removeIf(line -> isHintLine(line, previous));
        }
        lore.add(hintLine(command));
        meta.lore(lore);
        pdc.set(key, PersistentDataType.STRING, command);
        item.setItemMeta(meta);
    }

    /**
     * Removes any attached command tag and its lore line from {@code item}.
     *
     * @param plugin the owning plugin (for the PDC key's namespace)
     * @param item   the item to untag (its meta is replaced in place)
     * @return the command that was attached, or {@code null} if the item had none
     */
    static String detach(SEssentialsPlugin plugin, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey key = key(plugin);
        if (!pdc.has(key, PersistentDataType.STRING)) {
            return null;
        }
        String command = pdc.get(key, PersistentDataType.STRING);

        List<Component> lore = mutableLore(meta);
        lore.removeIf(line -> isHintLine(line, command));
        meta.lore(lore.isEmpty() ? null : lore);
        pdc.remove(key);
        item.setItemMeta(meta);
        return command;
    }

    /** @return a mutable copy of {@code meta}'s current lore (empty if it has none). */
    private static List<Component> mutableLore(ItemMeta meta) {
        List<Component> current = meta.lore();
        return current == null ? new ArrayList<>() : new ArrayList<>(current);
    }

    /** @return {@code true} if {@code line} is the "Right-click: /command" hint line for {@code command}. */
    private static boolean isHintLine(Component line, String command) {
        return PlainTextComponentSerializer.plainText().serialize(line)
                .equals(PlainTextComponentSerializer.plainText().serialize(hintLine(command)));
    }

    /**
     * Builds the "Right-click: /command" lore line. The label is small-capped and
     * coloured via {@link Style#HINT}; the command itself is appended as a plain,
     * unparsed literal in the same colour.
     */
    private static Component hintLine(String command) {
        Component label = MM.deserialize(Style.HINT + SmallCaps.of("Right-click:"))
                .decoration(TextDecoration.ITALIC, false);
        Component value = Component.text(" /" + command, label.color())
                .decoration(TextDecoration.ITALIC, false);
        return label.append(value);
    }
}
