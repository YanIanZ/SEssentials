package dev.iyanz.sessentials.module.kits;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * The kits feature module: {@code /kit}, {@code /kits} (alias {@code /kitlist}), and
 * the admin-only {@code /kit create}/{@code /kit delete} subcommands.
 *
 * <p>Kit definitions are persisted via {@link KitService} (backed by the
 * {@code kits} and {@code kitcooldowns} data stores); commands are registered and
 * implemented in {@link KitCommands}. On a fresh install (empty kit store), a
 * demonstrative {@code starter} kit is created automatically.</p>
 */
public final class KitsModule implements EssModule {

    private static final int DEFAULT_COOLDOWN_SECONDS = 3600;

    @Override
    public String name() {
        return "kits";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        int defaultCooldownSeconds = plugin.getConfig().getInt("kits.default-cooldown", DEFAULT_COOLDOWN_SECONDS);
        KitService service = new KitService(
                plugin.stores().get("kits"),
                plugin.stores().get("kitcooldowns"),
                defaultCooldownSeconds);
        service.ensureDefaults();

        new KitCommands(plugin, service).register();
    }
}
