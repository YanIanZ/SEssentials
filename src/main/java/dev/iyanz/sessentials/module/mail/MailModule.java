package dev.iyanz.sessentials.module.mail;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Offline-friendly player mail: {@code /mail send} leaves a message for any player
 * (online or offline), {@code /mail read} shows your own mail newest-first, and
 * {@code /mail clear} deletes it. Players are notified of unread mail on join.
 *
 * <p>Mail is persisted in the {@code "mail"} data store; see {@link MailService}
 * for the storage format and {@link MailListener} for the join-time notification.</p>
 */
public final class MailModule implements EssModule {

    @Override
    public String name() {
        return "mail";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new MailListener(plugin), plugin);
        MailCommands.register(plugin);
    }
}
