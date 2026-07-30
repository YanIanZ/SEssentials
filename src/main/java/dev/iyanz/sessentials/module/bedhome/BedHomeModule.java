package dev.iyanz.sessentials.module.bedhome;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.store.YamlStore;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;

/**
 * CMI-style "bed = home" micro-behaviour: when a player successfully lies down in a bed,
 * a home named {@code bed} is automatically saved at that bed, so {@code /home bed} always
 * returns to where they last slept (and vanilla respawn already anchors there too).
 *
 * <p>Writes into the same {@code home} store and {@code <uuid>.homes.<name>} path the
 * {@code module.home} feature reads (via {@link YamlStore#setLocation}), so the auto home
 * shows up in {@code /home}, {@code /homes} and the homes GUI like any other. It is a
 * fixed, reserved name that is overwritten each sleep and deliberately bypasses the
 * per-player home limit (it is automatic, not a slot the player chose to spend).</p>
 *
 * <p>Toggle with config {@code home.bed-home} (default {@code true}). Folia-safe:
 * {@link PlayerBedEnterEvent} fires on the sleeping player's own region thread, and the
 * store is monitor-guarded with an async disk write.</p>
 */
public final class BedHomeModule implements EssModule {

    /** Reserved auto-home name. */
    private static final String BED_HOME = "bed";

    @Override
    public String name() {
        return "bedhome";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new BedHomeListener(plugin), plugin);
    }

    /** Saves the {@code bed} home whenever a player lies down. */
    private static final class BedHomeListener implements Listener {

        private final SEssentialsPlugin plugin;

        private BedHomeListener(SEssentialsPlugin plugin) {
            this.plugin = plugin;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onBedEnter(PlayerBedEnterEvent event) {
            if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
                return; // only a successful sleep sets the home
            }
            if (!plugin.getConfig().getBoolean("home.bed-home", true)) {
                return;
            }
            Block bed = event.getBed();
            // Centre on the bed block, keep the player's facing so /home bed looks natural.
            Location where = bed.getLocation().add(0.5, 0.0, 0.5);
            where.setYaw(event.getPlayer().getLocation().getYaw());

            YamlStore store = plugin.stores().get("home");
            String path = event.getPlayer().getUniqueId() + ".homes." + BED_HOME;
            store.setLocation(path, where);
            store.save();

            Msg.info(event.getPlayer(), "Saved this bed as your \"bed\" home.");
        }
    }
}
