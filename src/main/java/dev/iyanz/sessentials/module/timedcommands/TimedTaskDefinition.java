package dev.iyanz.sessentials.module.timedcommands;

import java.util.List;

/**
 * An immutable scheduled-command task read from one entry under
 * {@code timed-commands.tasks} in {@code config.yml}.
 *
 * @param id              the config key identifying this task (used for logging and
 *                        {@code /timedcommands list})
 * @param intervalSeconds seconds between firings; converted to ticks ({@code * 20})
 *                        when the repeating task is scheduled
 * @param commands        console commands dispatched, in order, every time this task
 *                        fires; may be empty (the task then fires but does nothing)
 */
public record TimedTaskDefinition(String id, long intervalSeconds, List<String> commands) {
}
