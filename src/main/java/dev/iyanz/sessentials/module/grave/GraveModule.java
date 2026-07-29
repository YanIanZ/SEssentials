package dev.iyanz.sessentials.module.grave;

import java.util.List;
import java.util.UUID;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

/**
 * Death graves: when an eligible player dies, their dropped items are captured into a
 * private, retrievable "grave" instead of scattering across the ground.
 *
 * <p>Behaviour (all gated on {@code grave.enabled}, default {@code false}):</p>
 * <ul>
 *   <li>{@link GraveDeathListener} moves a dying player's drops into an in-memory
 *       {@link Grave} keyed on their UUID and tells them where they died. No world block
 *       is altered, so terrain is never griefed.</li>
 *   <li>{@code /grave} (permission {@code sessentials.grave}) opens the owner's grave in a
 *       chest GUI ({@link GraveMenu}); taking items removes them from the grave, and an
 *       emptied grave is discarded. Only the owner can open their own grave.</li>
 *   <li>A repeating async sweep (every {@value #SWEEP_PERIOD_TICKS} ticks / 20s) expires
 *       graves older than {@code grave.expire-seconds} (default 300): their remaining items
 *       are dropped at the death location on that location's region thread, then the grave
 *       is removed.</li>
 * </ul>
 *
 * <p><b>No duplication:</b> the grave is the single source of truth. Items only ever move
 * — from the grave into the open GUI, out of the GUI into the player, or from an expired
 * grave onto the ground — and are never copied. The GUI checks its items out of the
 * registry on open and returns whatever is left on close, and the expiry sweep skips any
 * grave that is currently open.</p>
 *
 * <p><b>In-memory only:</b> graves do not survive a restart. To avoid silently losing
 * items when the plugin unloads, {@link #disable} drops every remaining grave's items to
 * the ground.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class GraveModule implements EssModule {

    /** Sweep cadence in ticks (400 ticks = 20 seconds at 20 TPS). */
    private static final long SWEEP_PERIOD_TICKS = 400L;

    private SEssentialsPlugin plugin;
    private GraveSettings settings;
    private GraveRegistry registry;
    private ScheduledTask sweepTask;

    @Override
    public String name() {
        return "grave";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.plugin = plugin;
        this.settings = GraveSettings.load(plugin.getConfig());
        this.registry = new GraveRegistry();

        plugin.getServer().getPluginManager().registerEvents(new GraveDeathListener(settings, registry), plugin);
        plugin.getServer().getPluginManager().registerEvents(new GraveCloseListener(registry), plugin);

        plugin.commands(reg -> reg.register(Commands.literal("grave")
                .requires(s -> s.getSender().hasPermission("sessentials.grave"))
                .executes(Cmds.playerExec(player -> GraveMenu.open(plugin, player, registry)))
                .build(), "Open your death grave to reclaim its items"));

        this.sweepTask = Schedulers.asyncRepeating(plugin, this::sweep, SWEEP_PERIOD_TICKS, SWEEP_PERIOD_TICKS);
    }

    @Override
    public void disable(SEssentialsPlugin plugin) {
        if (sweepTask != null) {
            sweepTask.cancel();
            sweepTask = null;
        }
        if (registry == null) {
            return;
        }
        // Graves are in-memory only, so on unload drop whatever is left to the ground
        // rather than losing it. Items currently checked out to an open GUI live in that
        // inventory (not the registry) and stay with the viewing player.
        for (Grave grave : registry.drainAll()) {
            dropAt(grave.location(), grave.items());
        }
    }

    /**
     * One sweep over the currently registered graves: any that have expired (and are not
     * being viewed) are claimed and their items dropped at the death location. Runs on the
     * async scheduler and never touches world state directly — the drop is dispatched onto
     * the location's own region thread.
     */
    private void sweep() {
        long now = System.currentTimeMillis();
        for (UUID owner : registry.owners()) {
            Grave grave = registry.claimExpired(owner, now);
            if (grave != null) {
                dropAt(grave.location(), grave.items());
            }
        }
    }

    /**
     * Drops {@code items} naturally at {@code where}, on that location's region thread (as
     * required on Folia for world mutation). Null/empty items and unloaded worlds are
     * skipped.
     *
     * @param where the drop location
     * @param items the items to drop
     */
    private void dropAt(Location where, List<ItemStack> items) {
        if (where == null || where.getWorld() == null || items.isEmpty()) {
            return;
        }
        List<ItemStack> snapshot = items.stream().filter(i -> i != null && !i.getType().isAir()).toList();
        if (snapshot.isEmpty()) {
            return;
        }
        Bukkit.getRegionScheduler().execute(plugin, where, () -> {
            World world = where.getWorld();
            if (world == null) {
                return;
            }
            for (ItemStack item : snapshot) {
                world.dropItemNaturally(where, item);
            }
        });
    }
}
