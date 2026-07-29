package dev.iyanz.sessentials;

import java.util.List;

import dev.iyanz.sessentials.api.EssModule;

/**
 * The registry of feature modules to enable, in order. This is the single place that
 * lists modules; each module is otherwise self-contained.
 */
final class Modules {

    private Modules() {
    }

    static List<EssModule> all() {
        return List.of(
                new dev.iyanz.sessentials.module.teleport.TeleportModule(),
                new dev.iyanz.sessentials.module.home.HomeModule(),
                new dev.iyanz.sessentials.module.warp.WarpModule(),
                new dev.iyanz.sessentials.module.playerstate.PlayerStateModule(),
                new dev.iyanz.sessentials.module.items.ItemsModule(),
                new dev.iyanz.sessentials.module.worldtime.WorldTimeModule(),
                new dev.iyanz.sessentials.module.messaging.MessagingModule(),
                new dev.iyanz.sessentials.module.moderation.ModerationModule(),
                new dev.iyanz.sessentials.module.economy.EconomyModule(),
                new dev.iyanz.sessentials.module.kits.KitsModule(),
                new dev.iyanz.sessentials.module.identity.IdentityModule(),
                new dev.iyanz.sessentials.module.utility.UtilityModule()
        );
    }
}
