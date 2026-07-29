package dev.iyanz.sessentials.module.playtimerewards;

import java.util.List;

/**
 * An immutable playtime-reward definition read from one entry under
 * {@code playtime-rewards.rewards} in {@code config.yml}.
 *
 * @param id       the config key identifying this reward; also the token stored in a
 *                 player's "claimed" list once granted, so it must be stable across reloads
 * @param minutes  total played minutes (see {@link org.bukkit.Statistic#PLAY_ONE_MINUTE})
 *                 required before this reward is granted
 * @param commands console commands dispatched on grant, with {@code %player%} substituted
 *                 for the recipient's name; may be empty for a message-only reward
 * @param message  MiniMessage text sent to the recipient on grant; {@code null} or blank
 *                 means no message is sent
 */
public record RewardDefinition(String id, long minutes, List<String> commands, String message) {
}
