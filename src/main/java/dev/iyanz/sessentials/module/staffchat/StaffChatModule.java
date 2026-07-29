package dev.iyanz.sessentials.module.staffchat;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Staff-only chat channel guarded by the {@code sessentials.staffchat} permission.
 *
 * <p>Two ways to speak into the channel:</p>
 * <ul>
 *   <li>{@code /staffchat <message>} (alias {@code /schat}) — send a single message
 *       directly to the channel; or</li>
 *   <li>{@code /staffchat} with no arguments — toggle "staff-chat mode", after which the
 *       player's ordinary chat is diverted to the channel (see {@link StaffChatListener})
 *       until they toggle it off again.</li>
 * </ul>
 *
 * <p>Each channel line is delivered to the console and to every online holder of the
 * permission on that recipient's region thread, making the whole module Folia-safe. All
 * shared, in-memory state (the set of players in staff-chat mode) lives in
 * {@link StaffChatService}.</p>
 */
public final class StaffChatModule implements EssModule {

    private final StaffChatService service = new StaffChatService();

    @Override
    public String name() {
        return "staffchat";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new StaffChatListener(plugin, service), plugin);
        new StaffChatCommands(service).register(plugin);
    }
}
