package dev.iyanz.sessentials.module.kiteditor;

import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.module.kits.KitService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

/**
 * Clean-room, CMI-style <em>kit editor</em> GUI module (own layout and code). Registers
 * {@code /kiteditor <name>} (alias {@code /editkit}, permission
 * {@code sessentials.kiteditor}, player-only), which opens an editable chest GUI
 * pre-loaded with a kit's current items so an admin can drag items in/out visually and
 * save the result as the kit's new definition.
 *
 * <p>This module is a thin, reuse-only front-end for the kits feature: it does not touch
 * the {@code kits} module or re-implement any storage logic. It constructs its own
 * {@link KitService} bound to the very same shared {@code kits} / {@code kitcooldowns}
 * data stores (the {@code Stores} cache hands back the same {@code YamlStore} instances
 * the kits module uses), so every read and write goes through the exact same code path as
 * {@code /kit create} — kit definitions saved here are read back byte-for-byte compatibly
 * by {@code /kit <name>} and {@code /kits}.</p>
 *
 * <p>The store format targeted (owned by {@code KitService}): a kit's items are a native
 * Bukkit {@code List<ItemStack>} under {@code kits.<name>.items}, and its cooldown
 * (seconds) is an int under {@code kits.<name>.cooldown}, in {@code kits.yml}.</p>
 *
 * <p>Folia-safe: the GUI is a SELIB menu opened on the viewer's region thread, with clicks
 * and the close callback routed there by the globally-registered SELIB menu listener; the
 * store write on save happens inside a click handler and therefore on that same region
 * thread.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class KitEditorModule implements EssModule {

    /** Permission required to open the kit editor. */
    private static final String PERMISSION = "sessentials.kiteditor";

    /** Fallback cooldown (seconds) for a brand-new kit; mirrors the kits module's default. */
    private static final int DEFAULT_COOLDOWN_SECONDS = 3600;

    @Override
    public String name() {
        return "kiteditor";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        int defaultCooldownSeconds = plugin.getConfig().getInt("kits.default-cooldown", DEFAULT_COOLDOWN_SECONDS);
        // Same store names the kits module uses -> the Stores cache returns the same
        // YamlStore instances, so writes here are seen immediately by the kits module.
        KitService service = new KitService(
                plugin.stores().get("kits"),
                plugin.stores().get("kitcooldowns"),
                defaultCooldownSeconds);

        // Suggests existing kit names, but any word is accepted (so a new kit can be created).
        SuggestionProvider<CommandSourceStack> kitNames = (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            for (String kit : service.names()) {
                if (kit.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                    builder.suggest(kit);
                }
            }
            return builder.buildFuture();
        };

        plugin.commands(reg -> reg.register(Commands.literal("kiteditor")
                .requires(s -> s.getSender().hasPermission(PERMISSION))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(kitNames)
                        .executes(ctx -> open(plugin, service, ctx)))
                .build(),
                "Open an editable GUI to define a kit's items.",
                List.of("editkit")));
    }

    /** Resolves the viewer and opens the editor for the named kit (created lower-cased, as the kits module stores). */
    private int open(SEssentialsPlugin plugin, KitService service, CommandContext<CommandSourceStack> ctx) {
        Player viewer = Cmds.player(ctx);
        if (viewer == null) {
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "name").toLowerCase(Locale.ROOT);
        KitEditorMenu.open(plugin, service, viewer, name);
        return 1;
    }
}
