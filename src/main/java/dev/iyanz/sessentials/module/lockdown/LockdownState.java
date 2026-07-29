package dev.iyanz.sessentials.module.lockdown;

/**
 * Tiny in-memory holder for the server-wide lockdown flag and its reason.
 *
 * <p>State lives in two {@code static volatile} fields: {@code active} (written by the
 * {@code /lockdown} command on a command/region thread) and {@code reason} (the text
 * shown to blocked logins). Both are read by {@link LockdownListener} on the async
 * pre-login thread, so {@code volatile} guarantees each reader observes the latest
 * toggle without any locking. When activating, the reason is written <em>before</em>
 * the {@code active} flag flips to {@code true}, so any reader that sees the lockdown
 * as active is guaranteed (via the {@code volatile} happens-before ordering) to also
 * see the matching reason.</p>
 *
 * <p>State is intentionally session-only — a server restart clears the lockdown —
 * matching an operator's "hold the door while I fix something" use-case.</p>
 */
final class LockdownState {

    /** Reason applied when a lockdown is toggled on without an explicit message. */
    static final String DEFAULT_REASON = "Server is under maintenance.";

    private static volatile boolean active;
    private static volatile String reason = DEFAULT_REASON;

    private LockdownState() {
    }

    /** @return {@code true} if the server is currently locked down. */
    static boolean active() {
        return active;
    }

    /** @return the reason shown to blocked logins (meaningful only while {@link #active()}). */
    static String reason() {
        return reason;
    }

    /**
     * Turns the lockdown on with the given reason. The reason is published before the
     * active flag so readers never see an active lockdown with a stale reason.
     *
     * @param reason the reason to show blocked logins (already resolved, never null)
     */
    static void activate(String reason) {
        LockdownState.reason = reason;
        active = true;
    }

    /** Turns the lockdown off; new logins are allowed again. */
    static void deactivate() {
        active = false;
    }
}
