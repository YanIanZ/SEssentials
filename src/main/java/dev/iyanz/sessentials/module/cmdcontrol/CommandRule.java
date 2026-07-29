package dev.iyanz.sessentials.module.cmdcontrol;

/**
 * A single command label's control rule, as configured under
 * {@code command-control.commands.<label>} in {@code config.yml}.
 *
 * @param cooldownSeconds  seconds between uses ({@code 0} disables the cooldown check)
 * @param cost             money charged via Vault on each allowed use ({@code 0} disables charging)
 * @param bypassPermission the permission that exempts a player from both the cooldown and the cost
 */
record CommandRule(long cooldownSeconds, double cost, String bypassPermission) {
}
