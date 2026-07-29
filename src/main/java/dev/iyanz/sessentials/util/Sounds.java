package dev.iyanz.sessentials.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Small, consistent set of UI sound effects for SourbyBank. Each method plays a
 * short sound at the player's location; all calls must be made on the player's
 * region thread (menu clicks and service outcomes already are).
 */
public final class Sounds {

    private Sounds() {
    }

    /** A menu is opened. */
    public static void open(Player player) {
        play(player, Sound.BLOCK_BARREL_OPEN, 0.6f, 1.4f);
    }

    /** A clickable button was pressed. */
    public static void click(Player player) {
        play(player, Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }

    /** Coins moved successfully (deposit / withdraw). */
    public static void coins(Player player) {
        play(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.6f);
    }

    /** An action failed / was denied. */
    public static void fail(Player player) {
        play(player, Sound.ENTITY_VILLAGER_NO, 0.6f, 1.0f);
    }

    /** A bank upgrade succeeded. */
    public static void upgrade(Player player) {
        play(player, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.0f);
    }

    /** A piggy bank was right-clicked to open the bank. */
    public static void piggy(Player player) {
        play(player, Sound.BLOCK_ENDER_CHEST_OPEN, 0.6f, 1.3f);
    }

    /** The thief took someone else's piggy bank (played to the thief). */
    public static void steal(Player player) {
        play(player, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.5f);
        play(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 0.8f);
    }

    /** The owner's piggy bank was stolen (played to the victim). */
    public static void robbed(Player player) {
        play(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.6f);
        play(player, Sound.ENTITY_VILLAGER_NO, 0.7f, 0.9f);
    }

    /** Coins were lost (e.g. dropped from the purse on death). */
    public static void loss(Player player) {
        play(player, Sound.ENTITY_ITEM_BREAK, 0.8f, 0.8f);
        play(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.9f, 0.5f);
    }

    private static void play(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}
