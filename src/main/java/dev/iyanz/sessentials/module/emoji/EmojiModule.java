package dev.iyanz.sessentials.module.emoji;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Chat emoji module: expands {@code :code:} tokens (e.g. {@code :smile:} &rarr; 😊) in chat
 * and provides a {@code /emoji} browser GUI.
 *
 * <p>Configuration ({@code config.yml}):</p>
 * <pre>{@code
 * emoji:
 *   enabled: true          # master toggle (default true)
 *   map:                   # code -> unicode emoji; seeded with a default set if empty
 *     smile: "😊"
 *     heart: "❤"
 *     star: "⭐"
 *     fire: "🔥"
 *     check: "✔"
 *     cross: "✖"
 *     skull: "☠"
 *     arrow: "➜"
 * }</pre>
 *
 * <p>On enable the module seeds the default {@code map} (and the {@code enabled} flag) into
 * the config if absent, then — when enabled — registers the {@link EmojiChatListener} and
 * the {@code /emoji} command. The single {@link EmojiMap} built here is shared by both, so
 * the chat replacement and the GUI always reflect the same configured set.</p>
 *
 * <p>Permissions: {@code sessentials.emoji.use} gates chat expansion; {@code sessentials.emoji}
 * gates the {@code /emoji} command.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class EmojiModule implements EssModule {

    @Override
    public String name() {
        return "emoji";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        EmojiMap.seedDefaultsIfEmpty(plugin);

        if (!plugin.getConfig().getBoolean("emoji.enabled", true)) {
            return;
        }

        EmojiMap emojiMap = EmojiMap.load(plugin.getConfig());

        plugin.getServer().getPluginManager().registerEvents(new EmojiChatListener(emojiMap), plugin);

        plugin.commands(reg -> reg.register(Commands.literal("emoji")
                .requires(s -> s.getSender().hasPermission("sessentials.emoji"))
                .executes(Cmds.playerExec(player -> EmojiMenu.open(plugin, emojiMap, player)))
                .build(), "Browse the server's chat emojis"));
    }
}
