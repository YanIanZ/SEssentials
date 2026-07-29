package dev.iyanz.sessentials;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.economy.EconomyHook;
import dev.iyanz.sessentials.gui.MenuListener;
import dev.iyanz.sessentials.store.Stores;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Sourby Essentials entry point. Loads core services (config, messages, economy hook,
 * YAML data stores) and enables every {@link EssModule}; each module registers its own
 * commands and listeners, keeping features independent.
 */
@SuppressWarnings("UnstableApiUsage")
public final class SEssentialsPlugin extends JavaPlugin {

    private EconomyHook economy;
    private Stores stores;
    private final List<EssModule> modules = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Msg.init(getConfig().getString("prefix", "<gradient:#43C6AC:#4FC3F7><bold>SEssentials</bold></gradient> <dark_gray>» "));

        this.economy = new EconomyHook();
        this.stores = new Stores(this);

        getServer().getPluginManager().registerEvents(new MenuListener(), this);

        modules.addAll(Modules.all());
        int enabled = 0;
        for (EssModule module : modules) {
            try {
                module.enable(this);
                enabled++;
            } catch (Exception ex) {
                getLogger().severe("Module '" + module.name() + "' failed to enable: " + ex.getMessage());
            }
        }

        banner(enabled);
    }

    @Override
    public void onDisable() {
        for (EssModule module : modules) {
            try {
                module.disable(this);
            } catch (Exception ignored) {
                // best-effort shutdown
            }
        }
    }

    /**
     * Registers commands on the Paper command lifecycle. Modules call this from their
     * {@code enable} to add Brigadier command nodes.
     *
     * @param fn receives the command registrar
     */
    public void commands(Consumer<Commands> fn) {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> fn.accept(event.registrar()));
    }

    /** @return the Vault economy hook. */
    public EconomyHook economy() {
        return economy;
    }

    /** @return the YAML data-store manager. */
    public Stores stores() {
        return stores;
    }

    private void banner(int enabledModules) {
        MiniMessage mm = MiniMessage.miniMessage();
        var console = Bukkit.getConsoleSender();
        console.sendMessage(mm.deserialize("<gradient:#43C6AC:#4FC3F7>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        console.sendMessage(mm.deserialize("  <gradient:#43C6AC:#4FC3F7><bold>ꜱᴇꜱꜱᴇɴᴛɪᴀʟꜱ</bold></gradient> <#9AA0A6>v" + getPluginMeta().getVersion()));
        console.sendMessage(mm.deserialize("  <#9AA0A6>ᴍᴏᴅᴜʟᴇꜱ <#F5F5F5>" + enabledModules
                + "  <#9AA0A6>ᴠᴀᴜʟᴛ " + (economy.available() ? "<#8BE28B>ʏᴇꜱ" : "<#FF7B7B>ɴᴏ")));
        console.sendMessage(mm.deserialize("  <#8BE28B>✔ ᴇɴᴀʙʟᴇᴅ"));
        console.sendMessage(mm.deserialize("<gradient:#43C6AC:#4FC3F7>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
    }
}
