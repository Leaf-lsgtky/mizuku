package moe.shizuku.manager.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Which design system renders the app.
 */
enum class UiStyle(val value: Int) {
    Material(0),
    Miuix(1);

    companion object {
        fun fromValue(value: Int): UiStyle = entries.firstOrNull { it.value == value } ?: Miuix
    }
}

/**
 * Light/dark selection. Resolved against the system setting only when [FollowSystem].
 */
enum class DarkMode(val value: Int) {
    FollowSystem(0),
    Light(1),
    Dark(2);

    companion object {
        fun fromValue(value: Int): DarkMode = entries.firstOrNull { it.value == value } ?: FollowSystem
    }
}

/**
 * Where the accent color comes from.
 */
enum class ColorSource(val value: Int) {
    /** Stock palette of the active design system. */
    Default(0),

    /** Wallpaper-derived colors (Monet, Android 12+). */
    Wallpaper(1),

    /** A user-picked seed color. */
    Custom(2);

    companion object {
        fun fromValue(value: Int): ColorSource = entries.firstOrNull { it.value == value } ?: Default
    }
}

/**
 * Tonal palette generation style. Names match Miuix's `ThemePaletteStyle` so the two map by name.
 */
enum class PaletteStyle(val value: Int) {
    TonalSpot(0),
    Neutral(1),
    Vibrant(2),
    Expressive(3),
    Rainbow(4),
    FruitSalad(5),
    Monochrome(6),
    Fidelity(7),
    Content(8);

    /** Only these four styles have 2025-spec tone mappings; the rest silently fall back. */
    val supportsSpec2025: Boolean
        get() = this == TonalSpot || this == Neutral || this == Vibrant || this == Expressive

    companion object {
        fun fromValue(value: Int): PaletteStyle = entries.firstOrNull { it.value == value } ?: TonalSpot
    }
}

enum class ColorSpec(val value: Int) {
    Spec2021(0),
    Spec2025(1);

    companion object {
        fun fromValue(value: Int): ColorSpec = entries.firstOrNull { it.value == value } ?: Spec2025
    }
}

/**
 * A selectable accent seed. [color] is also the persisted identity of the entry.
 */
@Immutable
data class SeedColor(val key: String, val color: Color)

const val DEFAULT_SEED_COLOR: Int = 0xFF3F51B5.toInt()

val PresetSeedColors: List<SeedColor> = listOf(
    SeedColor("blue", Color(DEFAULT_SEED_COLOR)),
    SeedColor("indigo", Color(0xFF5C6BC0)),
    SeedColor("sky", Color(0xFF0288D1)),
    SeedColor("teal", Color(0xFF00897B)),
    SeedColor("green", Color(0xFF43A047)),
    SeedColor("lime", Color(0xFF7CB342)),
    SeedColor("amber", Color(0xFFFFB300)),
    SeedColor("orange", Color(0xFFF57C00)),
    SeedColor("red", Color(0xFFE53935)),
    SeedColor("pink", Color(0xFFD81B60)),
    SeedColor("purple", Color(0xFF8E24AA)),
    SeedColor("brown", Color(0xFF6D4C41)),
    SeedColor("grey", Color(0xFF546E7A)),
)

/**
 * The complete appearance configuration, as one immutable snapshot.
 *
 * Everything the theme needs lives here, so a single state read in the composition root is
 * enough to make every appearance option apply instantly.
 */
@Immutable
data class ThemeState(
    val uiStyle: UiStyle = UiStyle.Miuix,
    val darkMode: DarkMode = DarkMode.FollowSystem,
    val pureBlack: Boolean = false,
    val colorSource: ColorSource = ColorSource.Default,
    val seedColor: Int = DEFAULT_SEED_COLOR,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ColorSpec = ColorSpec.Spec2025,
    val enableBlur: Boolean = true,
    val enableFloatingBottomBar: Boolean = false,
    val enableFloatingBottomBarBlur: Boolean = false,
) {
    val isMiuix: Boolean get() = uiStyle == UiStyle.Miuix

    /** True when the palette is driven by a seed (custom or wallpaper) rather than the stock palette. */
    val isSeeded: Boolean get() = colorSource != ColorSource.Default

    /** The spec actually applied, accounting for styles that lack 2025 mappings. */
    val effectiveColorSpec: ColorSpec
        get() = if (colorSpec == ColorSpec.Spec2025 && !paletteStyle.supportsSpec2025) {
            ColorSpec.Spec2021
        } else {
            colorSpec
        }
}
