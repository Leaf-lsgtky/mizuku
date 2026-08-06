package moe.shizuku.manager.theme

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import moe.shizuku.manager.ShizukuSettings

/**
 * The single source of truth for appearance settings.
 *
 * Reads are backed by Compose snapshot state, so any composable that touches [state] is
 * re-invoked when a setting changes — that is what makes every appearance option apply
 * instantly, with no activity recreation.
 *
 * Writes go to [SharedPreferences] *and* update the snapshot immediately, rather than waiting
 * for the change listener to come back. The listener is still registered so that writes made
 * from anywhere else (or from another process) are picked up too.
 */
object ThemeStore {

    private val prefs: SharedPreferences get() = ShizukuSettings.getPreferences()

    var state by mutableStateOf(ThemeState())
        private set

    /**
     * Held in a field on purpose: [SharedPreferences] keeps only a weak reference to its
     * listeners, so a listener that isn't strongly referenced gets collected and silently
     * stops firing.
     */
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key in ObservedKeys) {
            state = read()
        }
    }

    /**
     * Loads the persisted state and starts observing. Must be called after
     * [ShizukuSettings.initialize].
     */
    fun initialize() {
        state = read()
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    private fun read(): ThemeState = ThemeState(
        uiStyle = UiStyle.fromValue(prefs.getInt(Keys.UI_STYLE, UiStyle.Miuix.value)),
        darkMode = DarkMode.fromValue(prefs.getInt(Keys.DARK_MODE, DarkMode.FollowSystem.value)),
        pureBlack = prefs.getBoolean(Keys.PURE_BLACK, false),
        colorSource = ColorSource.fromValue(prefs.getInt(Keys.COLOR_SOURCE, ColorSource.Default.value)),
        seedColor = prefs.getInt(Keys.SEED_COLOR, DEFAULT_SEED_COLOR),
        paletteStyle = PaletteStyle.fromValue(prefs.getInt(Keys.PALETTE_STYLE, PaletteStyle.TonalSpot.value)),
        colorSpec = ColorSpec.fromValue(prefs.getInt(Keys.COLOR_SPEC, ColorSpec.Spec2025.value)),
        enableBlur = prefs.getBoolean(Keys.ENABLE_BLUR, true),
        enableFloatingBottomBar = prefs.getBoolean(Keys.FLOATING_BOTTOM_BAR, false),
        enableFloatingBottomBarBlur = prefs.getBoolean(Keys.FLOATING_BOTTOM_BAR_BLUR, false),
    )

    private inline fun update(
        crossinline persist: SharedPreferences.Editor.() -> Unit,
        transform: (ThemeState) -> ThemeState,
    ) {
        // Update the snapshot first so the UI responds on this frame; the listener will
        // arrive later with the same value and be a no-op.
        state = transform(state)
        prefs.edit { persist() }
    }

    fun setUiStyle(value: UiStyle) =
        update({ putInt(Keys.UI_STYLE, value.value) }) { it.copy(uiStyle = value) }

    fun setDarkMode(value: DarkMode) =
        update({ putInt(Keys.DARK_MODE, value.value) }) { it.copy(darkMode = value) }

    fun setPureBlack(value: Boolean) =
        update({ putBoolean(Keys.PURE_BLACK, value) }) { it.copy(pureBlack = value) }

    fun setColorSource(value: ColorSource) =
        update({ putInt(Keys.COLOR_SOURCE, value.value) }) { it.copy(colorSource = value) }

    fun setSeedColor(argb: Int) =
        update({
            putInt(Keys.SEED_COLOR, argb)
            // Picking a color implies wanting it used.
            putInt(Keys.COLOR_SOURCE, ColorSource.Custom.value)
        }) { it.copy(seedColor = argb, colorSource = ColorSource.Custom) }

    fun setPaletteStyle(value: PaletteStyle) =
        update({ putInt(Keys.PALETTE_STYLE, value.value) }) { it.copy(paletteStyle = value) }

    fun setColorSpec(value: ColorSpec) =
        update({ putInt(Keys.COLOR_SPEC, value.value) }) { it.copy(colorSpec = value) }

    fun setEnableBlur(value: Boolean) =
        update({ putBoolean(Keys.ENABLE_BLUR, value) }) { it.copy(enableBlur = value) }

    fun setEnableFloatingBottomBar(value: Boolean) =
        update({ putBoolean(Keys.FLOATING_BOTTOM_BAR, value) }) { it.copy(enableFloatingBottomBar = value) }

    fun setEnableFloatingBottomBarBlur(value: Boolean) =
        update({ putBoolean(Keys.FLOATING_BOTTOM_BAR_BLUR, value) }) {
            it.copy(enableFloatingBottomBarBlur = value)
        }

    object Keys {
        const val UI_STYLE = "theme_mode"
        const val DARK_MODE = "night_mode_v2"
        const val PURE_BLACK = "black_night_theme"
        const val COLOR_SOURCE = "color_source"
        const val SEED_COLOR = "seed_color"
        const val PALETTE_STYLE = "palette_style"
        const val COLOR_SPEC = "color_spec"
        const val ENABLE_BLUR = "enable_blur"
        const val FLOATING_BOTTOM_BAR = "enable_floating_bottom_bar"
        const val FLOATING_BOTTOM_BAR_BLUR = "enable_floating_bottom_bar_blur"
    }

    private val ObservedKeys = setOf(
        Keys.UI_STYLE, Keys.DARK_MODE, Keys.PURE_BLACK, Keys.COLOR_SOURCE, Keys.SEED_COLOR,
        Keys.PALETTE_STYLE, Keys.COLOR_SPEC, Keys.ENABLE_BLUR,
        Keys.FLOATING_BOTTOM_BAR, Keys.FLOATING_BOTTOM_BAR_BLUR,
    )
}
