package dev.iyanz.sessentials.module.ctext;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Operator-defined reusable rich messages ("ctexts"). Staff author named MiniMessage
 * snippets in config (single line or a list of lines) and replay them on demand, so
 * frequently sent notices — rules, links, event blurbs — live in one place instead of
 * being retyped.
 *
 * <p>Commands (all gated behind {@code sessentials.ctext}):</p>
 * <ul>
 *   <li>{@code /ctext <name>} — send the named message to yourself.</li>
 *   <li>{@code /ctext <name> <player>} — send it to an online player.</li>
 *   <li>{@code /ctext <name> all} (or {@code /ctextall <name>}) — broadcast it to
 *       everyone and the console.</li>
 *   <li>{@code /ctextlist} — list the defined ctext names.</li>
 *   <li>{@code /ctext reload} — reload the config and re-read the ctexts.</li>
 * </ul>
 *
 * <p>Message bodies are parsed as MiniMessage verbatim (gradients,
 * {@code <click>}/{@code <hover>} events, etc.): the content is trusted, operator
 * authored rich text, not untrusted player chat. Configuration lives under the
 * {@code ctexts} section — see {@link CTexts} — and a demonstrative {@code rules}
 * entry is seeded on first run if the section is empty. Delivery is Folia-safe: each
 * recipient is messaged on its own region thread.</p>
 */
public final class CTextModule implements EssModule {

    @Override
    public String name() {
        return "ctext";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        CTexts.seedDefaultIfEmpty(plugin);
        CTextCommands.register(plugin, new CTexts(plugin));
    }
}
