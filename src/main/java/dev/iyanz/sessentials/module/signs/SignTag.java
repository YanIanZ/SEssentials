package dev.iyanz.sessentials.module.signs;

import java.util.Optional;

/**
 * The set of recognised action-sign tags. A sign becomes an "action sign" when its
 * first line, once colour-stripped, trimmed and lower-cased, equals one of these
 * bracketed tags exactly. {@link #permission()} is the {@code sessentials.signs.use.*}
 * node required to right-click-use a sign of that type (creating any action sign also
 * requires the shared {@code sessentials.signs.create} permission, checked by
 * {@link SignChangeListener}).
 */
enum SignTag {

    /** {@code [warp]}, line 2 = warp name. Teleports the clicker to that warp. */
    WARP("[warp]", "sessentials.signs.use.warp"),

    /** {@code [spawn]}. Teleports the clicker to the server spawn. */
    SPAWN("[spawn]", "sessentials.signs.use.spawn"),

    /** {@code [command]}, line 2 = a command (no leading {@code /}) run as the clicker. */
    COMMAND("[command]", "sessentials.signs.use.command"),

    /** {@code [heal]}. Fully heals the clicker. */
    HEAL("[heal]", "sessentials.signs.use.heal"),

    /** {@code [feed]}. Refills the clicker's hunger and saturation. */
    FEED("[feed]", "sessentials.signs.use.feed"),

    /** {@code [kit]}, line 2 = kit name. Grants that kit to the clicker. */
    KIT("[kit]", "sessentials.signs.use.kit");

    private final String tag;
    private final String permission;

    SignTag(String tag, String permission) {
        this.tag = tag;
        this.permission = permission;
    }

    /** @return the required {@code sessentials.signs.use.*} permission node to use this sign. */
    String permission() {
        return permission;
    }

    /**
     * Matches a normalised sign line (already colour-stripped, trimmed and
     * lower-cased) against every known tag.
     *
     * @param normalizedFirstLine the sign's first line, normalised for comparison
     * @return the matching tag, or empty if the line isn't a recognised action tag
     */
    static Optional<SignTag> match(String normalizedFirstLine) {
        if (normalizedFirstLine == null || normalizedFirstLine.isEmpty()) {
            return Optional.empty();
        }
        for (SignTag candidate : values()) {
            if (candidate.tag.equals(normalizedFirstLine)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }
}
