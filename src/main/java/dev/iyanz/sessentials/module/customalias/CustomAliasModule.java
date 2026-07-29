package dev.iyanz.sessentials.module.customalias;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * Registers operator-defined command aliases from config: each key under
 * {@code aliases} becomes a new, argument-less command that runs its configured
 * target command as whoever invoked the alias.
 *
 * <p>Example config:</p>
 * <pre>
 * aliases:
 *   hub: "warp hub"
 *   discord: "tell-a-link"
 * </pre>
 *
 * <p>The target command string may contain {@code %player%}, substituted with the
 * invoking sender's name before dispatch. A player runs the target via
 * {@link Player#performCommand(String)} (already on their own region thread, per the
 * Folia command-execution rules); any other sender (typically console) runs it via
 * {@link Bukkit#dispatchCommand(CommandSender, String)}. If {@code aliases} is unset
 * or empty, this module registers nothing.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class CustomAliasModule implements EssModule {

    /** Placeholder in a target command string, replaced with the invoking sender's name. */
    private static final String PLAYER_PLACEHOLDER = "%player%";

    @Override
    public String name() {
        return "customalias";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("aliases");
        if (section == null) {
            return;
        }
        List<String> aliasNames = new ArrayList<>(section.getKeys(false));
        if (aliasNames.isEmpty()) {
            return;
        }

        plugin.commands(reg -> {
            for (String aliasName : aliasNames) {
                String target = section.getString(aliasName);
                if (target == null || target.isBlank()) {
                    continue;
                }
                reg.register(Commands.literal(aliasName)
                        .executes(ctx -> run(ctx, target))
                        .build(), "Alias for \"" + target + "\"");
            }
        });
    }

    /** Runs {@code targetTemplate} (with {@code %player%} substituted) as the alias's invoking sender. */
    private static int run(CommandContext<CommandSourceStack> ctx, String targetTemplate) {
        CommandSender sender = ctx.getSource().getSender();
        String command = targetTemplate.replace(PLAYER_PLACEHOLDER, sender.getName());
        if (sender instanceof Player player) {
            player.performCommand(command);
        } else {
            Bukkit.dispatchCommand(sender, command);
        }
        return 1;
    }
}
