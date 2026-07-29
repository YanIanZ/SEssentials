package dev.iyanz.sessentials.module.silentchest;

import java.util.Locale;
import java.util.UUID;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.util.Style;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * A read/write snapshot of a container block's inventory, opened in place of the
 * real container so the vanilla open animation and sound (both driven by the block
 * entity's own open-viewer tracking, broadcast to every nearby player) never fire.
 *
 * <p>{@link #open} copies the container's current contents into a freshly created,
 * unrelated {@link Inventory} and shows <em>that</em> to the viewer — the real
 * block's inventory is only ever read, never handed to
 * {@link Player#openInventory}, so the server never associates a viewer with the
 * actual block entity. When the viewer closes this view,
 * {@link SilentChestCloseListener} calls {@link #writeBack(SEssentialsPlugin)},
 * which re-locates the block on its own region thread and copies the (possibly
 * edited) contents back into it, but only if the block is still a {@link Container}
 * of the same material — guarding against the chunk having unloaded or the block
 * having been broken/replaced while the view was open.</p>
 */
final class SilentChestView implements InventoryHolder {

    /** Bukkit chest-family inventories must be sized in multiples of this. */
    private static final int SLOT_STEP = 9;
    /** The largest chest-family inventory (a fully-merged double chest). */
    private static final int MAX_SIZE = 54;

    private final Inventory inventory;
    private final UUID worldId;
    private final int blockX;
    private final int blockY;
    private final int blockZ;
    private final Material expectedType;

    private SilentChestView(UUID worldId, int blockX, int blockY, int blockZ, Material expectedType,
            int size, String title) {
        this.worldId = worldId;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.expectedType = expectedType;
        // Safe to pass `this` here: CraftInventoryCustom only stores the reference
        // for later getHolder() calls, it never invokes anything on it.
        this.inventory = Bukkit.createInventory(this, size,
                MiniMessage.miniMessage().deserialize(Style.title(title)));
    }

    /**
     * Builds a snapshot copy of {@code container}'s current contents and opens it to
     * {@code player}. Must be called on the region thread that owns {@code block}
     * (true for the thread a {@code PlayerInteractEvent} on that block fires on, so
     * no scheduler hop is needed here).
     *
     * @param player    the viewer
     * @param block     the clicked container block
     * @param container {@code block}'s {@link Container} state
     */
    static void open(Player player, Block block, Container container) {
        Inventory source = container.getInventory();
        int size = roundedSize(source.getSize());
        String title = friendlyName(block.getType());

        SilentChestView view = new SilentChestView(block.getWorld().getUID(), block.getX(), block.getY(),
                block.getZ(), block.getType(), size, title);

        int copyCount = Math.min(size, source.getSize());
        for (int i = 0; i < copyCount; i++) {
            ItemStack item = source.getItem(i);
            view.inventory.setItem(i, item == null ? null : item.clone());
        }
        player.openInventory(view.inventory);
    }

    /**
     * Schedules the write-back of this view's (possibly edited) contents into the
     * real container block, on the region thread that owns it. Safe to call from
     * any thread (e.g. the closing viewer's own region thread).
     *
     * @param plugin the owning plugin
     */
    void writeBack(SEssentialsPlugin plugin) {
        World world = Bukkit.getWorld(worldId);
        if (world == null) {
            return; // world no longer loaded — discard edits
        }
        ItemStack[] edited = inventory.getContents();
        Bukkit.getRegionScheduler().execute(plugin, world, blockX >> 4, blockZ >> 4, () -> {
            Block block = world.getBlockAt(blockX, blockY, blockZ);
            if (block.getType() != expectedType) {
                return; // block broken/replaced while the view was open — discard edits
            }
            BlockState state = block.getState();
            if (!(state instanceof Container container)) {
                return; // no longer a container (shouldn't happen given the type check above)
            }
            Inventory target = container.getInventory();
            int writeCount = Math.min(edited.length, target.getSize());
            for (int i = 0; i < writeCount; i++) {
                target.setItem(i, edited[i]);
            }
        });
    }

    /** Rounds {@code rawSize} up to the nearest multiple of 9, clamped to {@code [9, 54]}. */
    private static int roundedSize(int rawSize) {
        int rounded = ((rawSize + SLOT_STEP - 1) / SLOT_STEP) * SLOT_STEP;
        rounded = Math.max(SLOT_STEP, rounded);
        return Math.min(rounded, MAX_SIZE);
    }

    /** Renders a block material like {@code TRAPPED_CHEST} as {@code "Trapped Chest"}. */
    private static String friendlyName(Material type) {
        String[] words = type.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder name = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return name.toString();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
