package dev.iyanz.sessentials.module.mutechat;

/**
 * Tiny in-memory holder for the server-wide chat-mute flag.
 *
 * <p>The flag is a single {@code static volatile boolean}: it is written by the
 * {@code /mutechat} command (typically on a command/region thread) and read by the
 * {@link MuteChatListener} on the async chat thread, so {@code volatile} guarantees
 * each reader sees the latest toggle without any locking. State is intentionally
 * session-only — a server restart clears the mute — matching a moderator's "cool the
 * room for a moment" use-case.</p>
 */
final class MuteChatState {

    private static volatile boolean muted;

    private MuteChatState() {
    }

    /** @return {@code true} if global chat is currently muted. */
    static boolean muted() {
        return muted;
    }

    /**
     * Flips the global chat-mute flag.
     *
     * @return the new state ({@code true} = now muted, {@code false} = now unmuted)
     */
    static boolean toggle() {
        boolean next = !muted;
        muted = next;
        return next;
    }
}
