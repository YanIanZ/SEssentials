package dev.iyanz.sessentials.module.fun;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Lighthearted, low-stakes commands: potion effects, random fireworks, dyeing leather
 * armor, player-head skulls and writable books.
 *
 * <p>Every command that mutates another player (an {@code [player]} target other than
 * the sender) hops onto that player's Folia region thread before touching entity/world
 * API. See the individual command classes in this package for details:</p>
 * <ul>
 *   <li>{@link EffectCommands} — {@code effect}</li>
 *   <li>{@link FireworkCommands} — {@code firework}</li>
 *   <li>{@link DyeCommands} — {@code dye}</li>
 *   <li>{@link SkullCommands} — {@code skull}/{@code head}</li>
 *   <li>{@link BookCommands} — {@code book} ({@code unsign}, {@code copy})</li>
 * </ul>
 */
public final class FunModule implements EssModule {

    @Override
    public String name() {
        return "fun";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        EffectCommands.register(plugin);
        FireworkCommands.register(plugin);
        DyeCommands.register(plugin);
        SkullCommands.register(plugin);
        BookCommands.register(plugin);
    }
}
