package dev.iyanz.sessentials.module.menus;

import java.util.Locale;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.selib.gui.Buttons;
import dev.iyanz.sessentials.selib.gui.ItemBuilder;
import dev.iyanz.sessentials.selib.gui.Menu;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.Style;
import org.bukkit.Material;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * SELIB chest GUI for setting the viewer's <em>personal</em> (client-side) time and weather.
 * These affect only the clicking player's own view of the world via
 * {@link Player#setPlayerTime(long, boolean)} and {@link Player#setPlayerWeather(WeatherType)},
 * so nothing on the server or for other players changes.
 *
 * <p>The top interior row carries six time presets (day, noon, sunset, night, midnight and a
 * reset back to the server time); the lower interior row carries three weather presets (clear,
 * rain and a reset back to the server weather). Clicking a preset applies it on the viewer's
 * region thread and re-renders the menu in place — the menu stays open and the just-picked
 * preset is highlighted with a glint.</p>
 *
 * <p>Opened by {@code /ptimemenu}; see {@link MenusModule}. Folia-safe via the {@link Menu}
 * base and {@link Schedulers#entity} for every personal time/weather mutation.</p>
 */
public final class TimeWeatherMenu extends Menu {

    /** Sentinel {@code ticks} value meaning "reset to server time". */
    private static final long RESET_TIME = -1L;

    /** A selectable personal-time preset. */
    private enum TimePreset {
        DAY("Day", 1_000L, 10, Material.LIME_DYE),
        NOON("Noon", 6_000L, 11, Material.YELLOW_DYE),
        SUNSET("Sunset", 12_000L, 12, Material.ORANGE_DYE),
        NIGHT("Night", 13_000L, 13, Material.BLUE_DYE),
        MIDNIGHT("Midnight", 18_000L, 14, Material.BLACK_DYE),
        RESET("Reset Time", RESET_TIME, 16, Material.CLOCK);

        private final String label;
        private final long ticks;
        private final int slot;
        private final Material icon;

        TimePreset(String label, long ticks, int slot, Material icon) {
            this.label = label;
            this.ticks = ticks;
            this.slot = slot;
            this.icon = icon;
        }
    }

    /** A selectable personal-weather preset; {@code weather == null} means "reset to server weather". */
    private enum WeatherPreset {
        CLEAR("Clear", WeatherType.CLEAR, 20, Material.SUNFLOWER),
        RAIN("Rain", WeatherType.DOWNFALL, 22, Material.WATER_BUCKET),
        RESET("Reset Weather", null, 24, Material.MILK_BUCKET);

        private final String label;
        private final WeatherType weather;
        private final int slot;
        private final Material icon;

        WeatherPreset(String label, WeatherType weather, int slot, Material icon) {
            this.label = label;
            this.weather = weather;
            this.slot = slot;
            this.icon = icon;
        }
    }

    private TimePreset activeTime;
    private WeatherPreset activeWeather;

    private TimeWeatherMenu(SEssentialsPlugin plugin, Player viewer) {
        super(plugin, viewer, MM.deserialize(Style.title("Time & Weather")), 4);
    }

    /**
     * Builds and opens the personal time/weather menu for {@code viewer}.
     *
     * @param plugin the owning plugin
     * @param viewer the player to show the menu to
     */
    public static void open(SEssentialsPlugin plugin, Player viewer) {
        new TimeWeatherMenu(plugin, viewer).open();
    }

    @Override
    protected void build() {
        clear();
        for (TimePreset preset : TimePreset.values()) {
            set(preset.slot, timeIcon(preset), event -> applyTime(preset));
        }
        for (WeatherPreset preset : WeatherPreset.values()) {
            set(preset.slot, weatherIcon(preset), event -> applyWeather(preset));
        }
        border(Buttons.filler());
        fill(Buttons.filler());
    }

    /** Builds a time-preset icon, glowing when it is the active selection. */
    private ItemStack timeIcon(TimePreset preset) {
        final boolean active = preset == activeTime;
        ItemBuilder builder = new ItemBuilder(preset.icon)
                .name(Style.button(Style.INFO, preset.label))
                .lore(active ? Style.lore("Active") : Style.hint("Click to apply"))
                .clean();
        if (active) {
            builder.glow();
        }
        return builder.build();
    }

    /** Builds a weather-preset icon, glowing when it is the active selection. */
    private ItemStack weatherIcon(WeatherPreset preset) {
        final boolean active = preset == activeWeather;
        ItemBuilder builder = new ItemBuilder(preset.icon)
                .name(Style.button(Style.INFO, preset.label))
                .lore(active ? Style.lore("Active") : Style.hint("Click to apply"))
                .clean();
        if (active) {
            builder.glow();
        }
        return builder.build();
    }

    /** Applies a personal-time preset on the viewer's region thread and re-renders in place. */
    private void applyTime(TimePreset preset) {
        Schedulers.entity(plugin, viewer, () -> {
            if (preset.ticks == RESET_TIME) {
                viewer.resetPlayerTime();
                Msg.ok(viewer, "Personal time reset.");
            } else {
                viewer.setPlayerTime(preset.ticks, false);
                Msg.value(viewer, "Personal time:", preset.label.toLowerCase(Locale.ROOT));
            }
            activeTime = preset == TimePreset.RESET ? null : preset;
            rerender();
        });
    }

    /** Applies a personal-weather preset on the viewer's region thread and re-renders in place. */
    private void applyWeather(WeatherPreset preset) {
        Schedulers.entity(plugin, viewer, () -> {
            if (preset.weather == null) {
                viewer.resetPlayerWeather();
                Msg.ok(viewer, "Personal weather reset.");
            } else {
                viewer.setPlayerWeather(preset.weather);
                Msg.value(viewer, "Personal weather:", preset.label.toLowerCase(Locale.ROOT));
            }
            activeWeather = preset == WeatherPreset.RESET ? null : preset;
            rerender();
        });
    }

    /**
     * Rebuilds the menu contents and pushes them to the open view. Runs on the viewer's region
     * thread (both the apply handlers that call it are scheduled there).
     */
    private void rerender() {
        build();
        viewer.updateInventory();
    }
}
