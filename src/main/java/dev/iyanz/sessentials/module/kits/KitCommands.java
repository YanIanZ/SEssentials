package dev.iyanz.sessentials.module.kits;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Registers and implements the kits command tree:
 * <ul>
 *   <li>{@code /kit} — lists available kits (no argument) or grants the named kit.</li>
 *   <li>{@code /kit create <name>} — saves the sender's current inventory as a kit
 *       (requires {@code sessentials.kit.admin}).</li>
 *   <li>{@code /kit delete <name>} — removes a kit (requires {@code sessentials.kit.admin}).</li>
 *   <li>{@code /kits} (alias {@code /kitlist}) — lists available kits and cooldown status.</li>
 * </ul>
 * Per-kit access is gated by the dynamic permission {@code sessentials.kit.<name>},
 * checked at execution time (Brigadier's {@code requires()} cannot see the parsed
 * argument value, so it cannot gate a per-kit node).
 */
@SuppressWarnings("UnstableApiUsage")
final class KitCommands {

    private static final String ADMIN_PERMISSION = "sessentials.kit.admin";

    private SEssentialsPlugin plugin;
    private KitService service;

    /** Suggests kit names the command sender has permission to use. */
    private final SuggestionProvider<CommandSourceStack> availableKits = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        CommandSender sender = ctx.getSource().getSender();
        for (String name : service.names()) {
            if (name.toLowerCase(Locale.ROOT).startsWith(remaining) && sender.hasPermission(kitPermission(name))) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    };

    /** Suggests every defined kit name, regardless of permission (for admin subcommands). */
    private final SuggestionProvider<CommandSourceStack> allKits = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (String name : service.names()) {
            if (name.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    };

    KitCommands(SEssentialsPlugin plugin, KitService service) {
        this.plugin = plugin;
        this.service = service;
    }

    /** Builds and registers {@code /kit} and {@code /kits} on the command lifecycle. */
    void register() {
        plugin.commands(reg -> {
            reg.register(Commands.literal("kit")
                    .executes(ctx -> {
                        listKits(ctx.getSource().getSender());
                        return 1;
                    })
                    .then(Commands.literal("create")
                            .requires(s -> s.getSender().hasPermission(ADMIN_PERMISSION))
                            .then(Commands.argument("name", StringArgumentType.word())
                                    .suggests(allKits)
                                    .executes(this::createKit)))
                    .then(Commands.literal("delete")
                            .requires(s -> s.getSender().hasPermission(ADMIN_PERMISSION))
                            .then(Commands.argument("name", StringArgumentType.word())
                                    .suggests(allKits)
                                    .executes(this::deleteKit)))
                    .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(availableKits)
                            .executes(this::giveKit))
                    .build(), "Give yourself a kit, or list kits with no argument.");

            reg.register(Commands.literal("kits")
                    .executes(ctx -> {
                        listKits(ctx.getSource().getSender());
                        return 1;
                    })
                    .build(), "List available kits and their cooldown status.", List.of("kitlist"));
        });
    }

    private int giveKit(CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "name").toLowerCase(Locale.ROOT);
        if (!player.hasPermission(kitPermission(name))) {
            Msg.err(player, "You don't have access to kit " + name + ".");
            return 0;
        }
        Optional<Kit> kitOpt = service.get(name);
        if (kitOpt.isEmpty()) {
            Msg.err(player, "No such kit: " + name + ".");
            return 0;
        }
        Kit kit = kitOpt.get();
        long remaining = service.remainingSeconds(player.getUniqueId(), name);
        if (remaining > 0) {
            Msg.err(player, "Kit " + name + " is on cooldown for " + formatDuration(remaining) + ".");
            return 0;
        }
        service.startCooldown(player.getUniqueId(), name, kit.cooldownSeconds());
        giveItems(player, kit.items());
        Msg.ok(player, "You received kit " + name + ".");
        return 1;
    }

    private int createKit(CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "name").toLowerCase(Locale.ROOT);
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null) {
                items.add(stack.clone());
            }
        }
        int cooldownSeconds = service.get(name).map(Kit::cooldownSeconds).orElseGet(service::defaultCooldownSeconds);
        service.save(name, items, cooldownSeconds);
        Msg.ok(player, "Saved kit " + name + " with " + items.size() + " item(s).");
        return 1;
    }

    private int deleteKit(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "name").toLowerCase(Locale.ROOT);
        if (!service.exists(name)) {
            Msg.err(sender, "No such kit: " + name + ".");
            return 0;
        }
        service.delete(name);
        Msg.ok(sender, "Deleted kit " + name + ".");
        return 1;
    }

    /** Lists kits {@code sender} has permission to use, with their cooldown status. */
    private void listKits(CommandSender sender) {
        List<String> names = service.names();
        if (names.isEmpty()) {
            Msg.info(sender, "There are no kits defined yet.");
            return;
        }
        boolean any = false;
        for (String name : names) {
            if (!sender.hasPermission(kitPermission(name))) {
                continue;
            }
            any = true;
            String status;
            if (sender instanceof Player player) {
                long remaining = service.remainingSeconds(player.getUniqueId(), name);
                status = remaining > 0 ? formatDuration(remaining) : "Ready";
            } else {
                status = "Ready";
            }
            Msg.value(sender, name + ":", status);
        }
        if (!any) {
            Msg.info(sender, "There are no kits available to you.");
        }
    }

    /** Grants (defensive copies of) {@code items} to {@code player}, dropping overflow on the ground. */
    private void giveItems(Player player, List<ItemStack> items) {
        ItemStack[] copies = new ItemStack[items.size()];
        for (int i = 0; i < items.size(); i++) {
            copies[i] = items.get(i).clone();
        }
        var overflow = player.getInventory().addItem(copies);
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private static String kitPermission(String name) {
        return "sessentials.kit." + name;
    }

    /** Formats whole seconds as a compact {@code "1h 5m 12s"}-style duration. */
    private static String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        StringBuilder out = new StringBuilder();
        if (hours > 0) {
            out.append(hours).append("h ");
        }
        if (hours > 0 || minutes > 0) {
            out.append(minutes).append("m ");
        }
        out.append(seconds).append("s");
        return out.toString();
    }
}
