@file:Suppress("RestrictedApi")

package moe.shizuku.manager.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.SchemeContent
import com.google.android.material.color.utilities.SchemeExpressive
import com.google.android.material.color.utilities.SchemeFidelity
import com.google.android.material.color.utilities.SchemeFruitSalad
import com.google.android.material.color.utilities.SchemeMonochrome
import com.google.android.material.color.utilities.SchemeNeutral
import com.google.android.material.color.utilities.SchemeRainbow
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.google.android.material.color.utilities.SchemeVibrant

/**
 * Builds a full Material 3 [ColorScheme] from a single seed color.
 *
 * Uses the HCT/quantizer utilities bundled with Material Components, which are the same
 * generators the framework uses for dynamic color — so no extra dependency is required.
 */
fun colorSchemeFromSeed(
    seed: Color,
    isDark: Boolean,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = 0.0,
): ColorScheme {
    val hct = Hct.fromInt(seed.toArgb())
    val scheme: DynamicScheme = when (style) {
        PaletteStyle.TonalSpot -> SchemeTonalSpot(hct, isDark, contrastLevel)
        PaletteStyle.Neutral -> SchemeNeutral(hct, isDark, contrastLevel)
        PaletteStyle.Vibrant -> SchemeVibrant(hct, isDark, contrastLevel)
        PaletteStyle.Expressive -> SchemeExpressive(hct, isDark, contrastLevel)
        PaletteStyle.Rainbow -> SchemeRainbow(hct, isDark, contrastLevel)
        PaletteStyle.FruitSalad -> SchemeFruitSalad(hct, isDark, contrastLevel)
        PaletteStyle.Monochrome -> SchemeMonochrome(hct, isDark, contrastLevel)
        PaletteStyle.Fidelity -> SchemeFidelity(hct, isDark, contrastLevel)
        PaletteStyle.Content -> SchemeContent(hct, isDark, contrastLevel)
    }

    val c = MaterialDynamicColors()
    fun role(get: MaterialDynamicColors.() -> com.google.android.material.color.utilities.DynamicColor) =
        Color(c.get().getArgb(scheme))

    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = role { primary() },
        onPrimary = role { onPrimary() },
        primaryContainer = role { primaryContainer() },
        onPrimaryContainer = role { onPrimaryContainer() },
        inversePrimary = role { inversePrimary() },
        secondary = role { secondary() },
        onSecondary = role { onSecondary() },
        secondaryContainer = role { secondaryContainer() },
        onSecondaryContainer = role { onSecondaryContainer() },
        tertiary = role { tertiary() },
        onTertiary = role { onTertiary() },
        tertiaryContainer = role { tertiaryContainer() },
        onTertiaryContainer = role { onTertiaryContainer() },
        background = role { background() },
        onBackground = role { onBackground() },
        surface = role { surface() },
        onSurface = role { onSurface() },
        surfaceVariant = role { surfaceVariant() },
        onSurfaceVariant = role { onSurfaceVariant() },
        surfaceTint = role { primary() },
        inverseSurface = role { inverseSurface() },
        inverseOnSurface = role { inverseOnSurface() },
        error = role { error() },
        onError = role { onError() },
        errorContainer = role { errorContainer() },
        onErrorContainer = role { onErrorContainer() },
        outline = role { outline() },
        outlineVariant = role { outlineVariant() },
        scrim = role { scrim() },
        surfaceBright = role { surfaceBright() },
        surfaceDim = role { surfaceDim() },
        surfaceContainer = role { surfaceContainer() },
        surfaceContainerHigh = role { surfaceContainerHigh() },
        surfaceContainerHighest = role { surfaceContainerHighest() },
        surfaceContainerLow = role { surfaceContainerLow() },
        surfaceContainerLowest = role { surfaceContainerLowest() },
    )
}

/**
 * Flattens background and surface-container roles to true black for OLED panels.
 */
fun ColorScheme.withPureBlack(enabled: Boolean): ColorScheme =
    if (!enabled) this else copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF0D0D0D),
        surfaceContainer = Color(0xFF111111),
        surfaceContainerHigh = Color(0xFF1A1A1A),
        surfaceContainerHighest = Color(0xFF222222),
    )
