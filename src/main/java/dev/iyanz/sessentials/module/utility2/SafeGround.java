package dev.iyanz.sessentials.module.utility2;

import org.bukkit.block.Block;

/**
 * The "is this a safe place to stand" check shared by this module's teleport
 * helpers ({@code /up}, {@code /down}, {@code /rtp}). A block is safe ground when it
 * is solid: Bukkit's {@link org.bukkit.Material#isSolid()} already excludes air and
 * liquids, since water and lava have no collision box and don't block movement.
 */
final class SafeGround {

    private SafeGround() {
    }

    /**
     * @param block the block to test
     * @return {@code true} if {@code block} is solid ground (not air, not a liquid)
     */
    static boolean isSafe(Block block) {
        return block.getType().isSolid();
    }
}
