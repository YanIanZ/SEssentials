package dev.iyanz.sessentials.module.report;

import java.util.List;
import java.util.UUID;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.store.YamlStore;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.SmallCaps;
import dev.iyanz.sessentials.util.Style;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Player-report system: anyone with {@code sessentials.report} files a report against
 * another player via {@code /report <player> <reason>}, and staff holding
 * {@code sessentials.report.receive} review the queue with {@code /reports} or clear it
 * with {@code /reports clear} ({@code sessentials.report.admin}).
 *
 * <p>Targets are resolved through {@link Bukkit#getOfflinePlayer(String)} so offline
 * players can be reported; the report itself is stored by name (see {@link Reports}).
 * When a report is filed, every online staff member is notified on their own region
 * thread, keeping the notification Folia-safe.</p>
 *
 * <p>Report reasons are untrusted free text, so they are always appended as a plain
 * {@link Component#text(String)} rather than parsed as MiniMessage &mdash; a reason can
 * never inject colour tags, click events or similar markup into staff chat.</p>
 */
@SuppressWarnings({"UnstableApiUsage", "deprecation"})
public final class ReportModule implements EssModule {

    /** Permission required to file a report. */
    private static final String PERM_REPORT = "sessentials.report";
    /** Permission required to receive report notifications and view the queue. */
    private static final String PERM_RECEIVE = "sessentials.report.receive";
    /** Permission required to clear the report queue. */
    private static final String PERM_ADMIN = "sessentials.report.admin";
    /** Maximum number of reports listed by {@code /reports}. */
    private static final int LIST_LIMIT = 20;

    private SEssentialsPlugin plugin;
    private YamlStore store;

    @Override
    public String name() {
        return "report";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.plugin = plugin;
        this.store = plugin.stores().get("reports");

        plugin.commands(reg -> {
            reg.register(Commands.literal("report")
                    .requires(s -> s.getSender().hasPermission(PERM_REPORT))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .then(Commands.argument("reason", StringArgumentType.greedyString())
                                    .executes(this::report)))
                    .build(), "Report a player to staff");

            reg.register(Commands.literal("reports")
                    .requires(s -> s.getSender().hasPermission(PERM_RECEIVE))
                    .executes(this::list)
                    .then(Commands.literal("clear")
                            .requires(s -> s.getSender().hasPermission(PERM_ADMIN))
                            .executes(this::clear))
                    .build(), "List or clear player reports");
        });
    }

    // --- commands ------------------------------------------------------------

    /** Files a report against the named player and notifies online staff. */
    private int report(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        String reason = StringArgumentType.getString(ctx, "reason");

        if (sender instanceof Player self && self.getName().equalsIgnoreCase(name)) {
            Msg.err(sender, "You cannot report yourself.");
            return 0;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        String reportedName = target.getName() != null ? target.getName() : name;

        Reports.Entry entry = Reports.add(store, sender.getName(), reportedName, reason);
        Msg.ok(sender, "Reported " + reportedName + " to staff.");
        notifyStaff(sender, entry);
        return 1;
    }

    /** Lists the most recent reports, newest first, for reviewing staff. */
    private int list(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        List<Reports.Entry> recent = Reports.recent(store, LIST_LIMIT);
        if (recent.isEmpty()) {
            Msg.info(sender, "There are no reports.");
            return 1;
        }

        int total = Reports.count(store);
        Msg.info(sender, "Recent reports (showing " + recent.size() + " of " + total + "):");
        for (int i = 0; i < recent.size(); i++) {
            sender.sendMessage(line(i + 1, recent.get(i)));
        }
        return 1;
    }

    /** Clears the entire report queue. */
    private int clear(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        int count = Reports.count(store);
        if (count == 0) {
            Msg.info(sender, "There are no reports to clear.");
            return 1;
        }
        Reports.clear(store);
        Msg.ok(sender, "Cleared " + count + " report" + (count == 1 ? "" : "s") + ".");
        return 1;
    }

    // --- helpers -------------------------------------------------------------

    /**
     * Sends a notification for {@code entry} to every online staff member holding
     * {@link #PERM_RECEIVE}, each on their own region thread (Folia-safe). The reporter,
     * if online, is skipped since they already receive a direct confirmation.
     */
    private void notifyStaff(CommandSender reporter, Reports.Entry entry) {
        Component notice = notification(entry);
        UUID reporterId = reporter instanceof Player p ? p.getUniqueId() : null;
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (reporterId != null && staff.getUniqueId().equals(reporterId)) {
                continue;
            }
            if (!staff.hasPermission(PERM_RECEIVE)) {
                continue;
            }
            Schedulers.entity(plugin, staff, () -> staff.sendMessage(notice));
        }
    }

    /** {@code [new report] Reporter reported Target: reason} — reason kept as plain text. */
    private static Component notification(Reports.Entry entry) {
        Component prefix = Msg.mm(Style.BAD + "[" + SmallCaps.of("new report") + "] "
                + Style.VALUE + SmallCaps.of(entry.reporterName())
                + Style.GRAY + SmallCaps.of(" reported ")
                + Style.VALUE + SmallCaps.of(entry.reportedName()) + Style.GRAY + ": ");
        return prefix.append(Component.text(entry.reason(), NamedTextColor.WHITE));
    }

    /** {@code [1 • 3h ago] Reporter reported Target: reason} — reason kept as plain text. */
    private static Component line(int number, Reports.Entry entry) {
        String ago = Reports.relative(entry.epochMillis());
        Component prefix = Msg.mm(Style.GRAY + "[" + Style.INFO + number + Style.GRAY + " • " + ago + " ago] "
                + Style.VALUE + SmallCaps.of(entry.reporterName())
                + Style.GRAY + SmallCaps.of(" reported ")
                + Style.VALUE + SmallCaps.of(entry.reportedName()) + Style.GRAY + ": ");
        return prefix.append(Component.text(entry.reason(), NamedTextColor.WHITE));
    }
}
