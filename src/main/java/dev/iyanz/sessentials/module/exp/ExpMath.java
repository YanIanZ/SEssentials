package dev.iyanz.sessentials.module.exp;

/**
 * Converts a player's level/progress pair into the equivalent total experience
 * point count, using vanilla Minecraft's experience curve.
 *
 * <p>Bukkit's {@code Player#getTotalExperience()} tracks a separate lifetime
 * counter that can drift from level/progress (e.g. after direct
 * {@code setLevel}/{@code setExp} calls), so {@link ExpModule} derives "total exp"
 * purely from level + progress instead, which is also what the {@code give}/
 * {@code set}/{@code take} subcommands operate on.</p>
 */
final class ExpMath {

    private ExpMath() {
    }

    /** @return the experience points required to advance from {@code level} to {@code level + 1}. */
    static int pointsForLevel(int level) {
        if (level >= 31) {
            return 9 * level - 158;
        }
        if (level >= 16) {
            return 5 * level - 38;
        }
        return 2 * level + 7;
    }

    /** @return the cumulative experience points required to reach {@code level} starting from level 0. */
    static long pointsToReach(int level) {
        if (level >= 32) {
            return Math.round(4.5 * level * level - 162.5 * level + 2220);
        }
        if (level >= 17) {
            return Math.round(2.5 * level * level - 40.5 * level + 360);
        }
        return (long) level * level + 6L * level;
    }

    /**
     * @param level    the player's current level
     * @param progress the player's progress toward the next level, in {@code [0, 1)}
     * @return the equivalent total experience points for {@code level} + {@code progress}
     */
    static long totalExperience(int level, float progress) {
        return pointsToReach(level) + (long) Math.floor(progress * pointsForLevel(level));
    }
}
