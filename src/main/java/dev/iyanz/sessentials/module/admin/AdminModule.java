package dev.iyanz.sessentials.module.admin;

import java.io.File;
import java.util.function.Consumer;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Admin commands, including data migration from other plugins:
 * {@code /sess import <cmi|essentials>} pulls homes (and, for Essentials, warps) into
 * SEssentials' data stores. Imports run off-thread and never overwrite an existing
 * home/warp of the same name.
 */
@SuppressWarnings("UnstableApiUsage")
public final class AdminModule implements EssModule {

    private SEssentialsPlugin plugin;

    @Override
    public String name() {
        return "admin";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.plugin = plugin;
        plugin.commands(reg -> {
            reg.register(Commands.literal("sess")
                    .requires(s -> s.getSender().hasPermission("sessentials.admin"))
                    .then(Commands.literal("import")
                            .then(Commands.literal("cmi").executes(ctx -> runImport(ctx, "cmi")))
                            .then(Commands.literal("essentials").executes(ctx -> runImport(ctx, "essentials"))))
                    .then(Commands.literal("reload").executes(ctx -> {
                        plugin.reloadConfig();
                        Msg.ok(ctx.getSource().getSender(), "SEssentials config reloaded.");
                        return 1;
                    }))
                    .build(), "SEssentials admin");
        });
    }

    private int runImport(CommandContext<CommandSourceStack> ctx, String source) {
        CommandSender sender = ctx.getSource().getSender();
        File pluginsDir = plugin.getDataFolder().getParentFile();
        Msg.info(sender, "Importing from " + source + "… this runs in the background.");
        Consumer<String> report = summary -> reply(sender, summary);
        Schedulers.async(plugin, task -> {
            try {
                if (source.equals("cmi")) {
                    new CmiImporter(plugin).run(new File(pluginsDir, "CMI/cmi.sqlite.db"), report);
                } else {
                    new EssentialsImporter(plugin).run(new File(pluginsDir, "Essentials"), report);
                }
            } catch (Exception ex) {
                report.accept("Import failed: " + ex.getMessage());
            }
        });
        return 1;
    }

    private void reply(CommandSender sender, String summary) {
        if (sender instanceof Player p) {
            Schedulers.entity(plugin, p, () -> Msg.ok(p, summary));
        } else {
            Msg.ok(sender, summary);
        }
    }
}
