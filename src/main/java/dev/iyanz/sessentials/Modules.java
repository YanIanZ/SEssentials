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
                new dev.iyanz.sessentials.module.utility.UtilityModule(),
                new dev.iyanz.sessentials.module.admin.AdminModule(),
                new dev.iyanz.sessentials.module.jail.JailModule(),
                new dev.iyanz.sessentials.module.mail.MailModule(),
                new dev.iyanz.sessentials.module.ignore.IgnoreModule(),
                new dev.iyanz.sessentials.module.playtime.PlaytimeModule(),
                new dev.iyanz.sessentials.module.info.InfoModule(),
                new dev.iyanz.sessentials.module.worlds.WorldsModule(),
                new dev.iyanz.sessentials.module.chat.ChatModule(),
                new dev.iyanz.sessentials.module.inspect.InspectModule(),
                new dev.iyanz.sessentials.module.fun.FunModule(),
                new dev.iyanz.sessentials.module.warnings.WarningsModule(),
                new dev.iyanz.sessentials.module.combat.CombatModule(),
                new dev.iyanz.sessentials.module.econextra.EconExtraModule(),
                new dev.iyanz.sessentials.module.utility2.Utility2Module(),
                new dev.iyanz.sessentials.module.scavenger.ScavengerModule(),
                new dev.iyanz.sessentials.module.exp.ExpModule(),
                new dev.iyanz.sessentials.module.deathmessages.DeathMessagesModule(),
                new dev.iyanz.sessentials.module.autobroadcast.AutoBroadcastModule(),
                new dev.iyanz.sessentials.module.customalias.CustomAliasModule(),
                new dev.iyanz.sessentials.module.cmdcontrol.CmdControlModule(),
                new dev.iyanz.sessentials.module.holograms.HologramsModule(),
                new dev.iyanz.sessentials.module.portals.PortalsModule(),
                new dev.iyanz.sessentials.module.signs.SignsModule(),
                new dev.iyanz.sessentials.module.recipes.RecipesModule(),
                new dev.iyanz.sessentials.module.playtimerewards.PlaytimeRewardsModule(),
                new dev.iyanz.sessentials.module.placeholders.PlaceholdersModule(),
                new dev.iyanz.sessentials.module.chatformat.ChatFormatModule(),
                new dev.iyanz.sessentials.module.chatfilter.ChatFilterModule(),
                new dev.iyanz.sessentials.module.playeroptions.PlayerOptionsModule(),
                new dev.iyanz.sessentials.module.notify.NotifyModule(),
                new dev.iyanz.sessentials.module.worldtools.WorldToolsModule(),
                new dev.iyanz.sessentials.module.invtools.InvToolsModule(),
                new dev.iyanz.sessentials.module.hpbar.HpBarModule(),
                new dev.iyanz.sessentials.module.painting.PaintingModule(),
                new dev.iyanz.sessentials.module.armorstand.ArmorStandModule(),
                new dev.iyanz.sessentials.module.disableenchant.DisableEnchantModule(),
                new dev.iyanz.sessentials.module.colorlimits.ColorLimitsModule(),
                new dev.iyanz.sessentials.module.timedcommands.TimedCommandsModule(),
                new dev.iyanz.sessentials.module.savedinv.SavedInvModule(),
                new dev.iyanz.sessentials.module.nbt.NbtModule(),
                new dev.iyanz.sessentials.module.stats.StatsModule(),
                new dev.iyanz.sessentials.module.attachedcommands.AttachedCommandsModule(),
                new dev.iyanz.sessentials.module.ctellraw.CTellrawModule(),
                new dev.iyanz.sessentials.module.armoreffects.ArmorEffectsModule(),
                new dev.iyanz.sessentials.module.elytra.ElytraModule(),
                new dev.iyanz.sessentials.module.ipinfo.IpInfoModule()
        );
    }
}
