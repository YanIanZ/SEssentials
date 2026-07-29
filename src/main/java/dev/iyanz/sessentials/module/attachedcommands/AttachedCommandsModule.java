package dev.iyanz.sessentials.module.attachedcommands;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Lets operators bind a command to any item: {@code /attachcommand <command...>}
 * (alias {@code /attachcmd}) tags the item in the sender's hand so right-clicking it
 * runs the given command as the holder (with {@code %player%} substituted for their
 * name), and {@code /detachcommand} (alias {@code /detachcmd}) removes the binding.
 *
 * <p>See {@link AttachedCommandItems} for the item tagging, {@link AttachedCommandCommands}
 * for the commands, and {@link AttachedCommandListener} for the right-click trigger.</p>
 */
public final class AttachedCommandsModule implements EssModule {

    @Override
    public String name() {
        return "attachedcommands";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new AttachedCommandListener(plugin), plugin);
        AttachedCommandCommands.register(plugin);
    }
}
