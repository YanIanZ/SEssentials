package dev.iyanz.sessentials.module.signs;

import java.util.Locale;
import java.util.Optional;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Handles right-clicking an action sign: reads the front side's lines, matches the
 * first against a {@link SignTag}, checks the tag's {@code sessentials.signs.use.*}
 * permission, then runs the action on the clicking player's region thread (required on
 * Folia for any entity/world mutation) and cancels the interaction.
 */
final class SignInteractListener implements Listener {

    private final SEssentialsPlugin plugin;

    /** @param plugin the owning plugin, used to hop to the clicker's region thread and reach stores */
    SignInteractListener(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        BlockState state = block.getState();
        if (!(state instanceof Sign sign)) {
            return;
        }

        String firstLine = plainLine(sign, 0).trim().toLowerCase(Locale.ROOT);
        Optional<SignTag> tagOpt = SignTag.match(firstLine);
        if (tagOpt.isEmpty()) {
            return;
        }
        SignTag tag = tagOpt.get();
        Player player = event.getPlayer();
        event.setCancelled(true);

        if (!player.hasPermission(tag.permission())) {
            Msg.err(player, "You don't have permission to use this sign.");
            return;
        }
        String argument = plainLine(sign, 1).trim();
        Schedulers.entity(plugin, player, () -> SignActions.run(plugin, player, tag, argument));
    }

    /** @return the plain text of {@code sign}'s front-side line {@code index} (never {@code null}). */
    private static String plainLine(Sign sign, int index) {
        return PlainTextComponentSerializer.plainText().serialize(sign.getSide(Side.FRONT).line(index));
    }
}
