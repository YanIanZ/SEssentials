package dev.iyanz.sessentials.module.slowmode;

/**
 * Mutable holder for the single global chat-slowmode value, shared between the
 * {@link SlowModeModule} command (which writes it) and the {@link SlowModeListener}
 * (which reads it on every chat message).
 *
 * <p>The value is the minimum number of seconds a non-exempt player must wait between
 * chat messages; {@code 0} disables slowmode entirely. The field is {@code volatile} so
 * a change made on a command thread is seen immediately by the off-region chat threads
 * that consult it, with no locking required.</p>
 */
final class SlowModeState {

    /** Current global slowmode gap in seconds; {@code 0} means slowmode is off. */
    private volatile int seconds;

    /** @return the current slowmode gap in seconds ({@code 0} when disabled). */
    int seconds() {
        return seconds;
    }

    /**
     * Sets the global slowmode gap.
     *
     * @param seconds the new gap in seconds; {@code 0} disables slowmode
     */
    void seconds(int seconds) {
        this.seconds = seconds;
    }
}
