package dev.iyanz.sessentials.module.ctellraw;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Operator raw-MiniMessage messaging module: {@code /ctellraw} to send a rich
 * message to a single online player, and {@code /ctellrawall} to broadcast one to
 * every online player and the console.
 *
 * <p>Both commands are gated behind the {@code sessentials.ctellraw} permission and
 * parse their message argument as MiniMessage verbatim (gradients, click/hover
 * events, etc.), unlike the plain-text {@code Msg.ok}/{@code Msg.err}/{@code Msg.info}
 * helpers used by most other modules. See {@link CTellrawCommands} for details.</p>
 */
public final class CTellrawModule implements EssModule {

    @Override
    public String name() {
        return "ctellraw";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        CTellrawCommands.register(plugin);
    }
}
