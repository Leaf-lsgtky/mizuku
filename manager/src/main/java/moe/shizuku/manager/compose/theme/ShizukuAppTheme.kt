package moe.shizuku.manager.compose.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import moe.shizuku.manager.theme.ColorSource
import moe.shizuku.manager.theme.DarkMode
import moe.shizuku.manager.theme.PaletteStyle
import moe.shizuku.manager.theme.ThemeState
import moe.shizuku.manager.theme.ThemeStore
import moe.shizuku.manager.theme.colorSchemeFromSeed
import moe.shizuku.manager.theme.withPureBlack
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec as MiuixColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController as MiuixThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle as MiuixPaletteStyle

/** The active appearance configuration, readable from anywhere in the tree. */
val LocalThemeState = staticCompositionLocalOf { ThemeState() }

/** True when the app is currently rendering dark, regardless of how that was decided. */
val LocalIsDark = staticCompositionLocalOf { false }

/**
 * Retained for existing call sites that only need to know which design system is active.
 */
val LocalIsMiuix = staticCompositionLocalOf { false }

object AppTheme {
    val state: ThemeState
        @Composable @ReadOnlyComposable get() = LocalThemeState.current

    val isDark: Boolean
        @Composable @ReadOnlyComposable get() = LocalIsDark.current

    val isMiuix: Boolean
        @Composable @ReadOnlyComposable get() = LocalIsMiuix.current
}

/**
 * Root theme for the whole app.
 *
 * Reads appearance settings from [ThemeStore], whose state is Compose snapshot state. Any
 * change to a setting recomposes this function and repaints the app on the next frame — no
 * activity recreation, and no restart.
 */
@Composable
fun ShizukuAppTheme(
    state: ThemeState = ThemeStore.state,
    content: @Composable () -> Unit,
) {
    val isDark = when (state.darkMode) {
        DarkMode.Light -> false
        DarkMode.Dark -> true
        DarkMode.FollowSystem -> isSystemInDarkTheme()
    }

    SystemBarsEffect(isDark)

    CompositionLocalProvider(
        LocalThemeState provides state,
        LocalIsDark provides isDark,
        LocalIsMiuix provides state.isMiuix,
    ) {
        if (state.isMiuix) {
            MiuixAppTheme(state, isDark, content)
        } else {
            MaterialAppTheme(state, isDark, content)
        }
    }
}

/**
 * Keeps status/navigation bar icon contrast in step with the resolved theme.
 *
 * Keyed on [isDark] so it re-runs whenever the theme flips, which is what lets the bars follow
 * an in-app theme change without the activity being recreated.
 */
@Composable
private fun SystemBarsEffect(isDark: Boolean) {
    val context = LocalContext.current
    DisposableEffect(isDark) {
        val window = (context as? Activity)?.window
        if (window != null) {
            WindowInsetsControllerCompat(window, window.decorView).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
        onDispose { }
    }
}

/**
 * Provides a Miuix theme matching the current appearance settings.
 *
 * Needed by screens built from Miuix widgets that can also be shown while the app is in
 * Material mode — without this the widgets fall back to Miuix's default light palette.
 */
@Composable
fun ProvideMiuixTheme(content: @Composable () -> Unit) {
    val state = LocalThemeState.current
    val isDark = LocalIsDark.current
    if (state.isMiuix) content() else MiuixAppTheme(state, isDark, content)
}

@Composable
private fun MaterialAppTheme(
    state: ThemeState,
    isDark: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val target = remember(state.colorSource, state.seedColor, state.paletteStyle, isDark, state.pureBlack) {
        when {
            // Wallpaper colors: on API 31+ take the framework's own scheme directly.
            state.colorSource == ColorSource.Wallpaper && supportsDynamic ->
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

            state.isSeeded -> colorSchemeFromSeed(
                seed = Color(state.seedColor),
                isDark = isDark,
                style = state.paletteStyle,
            )

            else -> colorSchemeFromSeed(
                seed = Color(moe.shizuku.manager.theme.DEFAULT_SEED_COLOR),
                isDark = isDark,
                style = PaletteStyle.TonalSpot,
            )
        }.withPureBlack(isDark && state.pureBlack)
    }

    MaterialTheme(
        colorScheme = target.animated(),
        content = content,
    )
}

@Composable
private fun MiuixAppTheme(
    state: ThemeState,
    isDark: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // Miuix generates its own palette, so it takes the same seed/style/spec inputs as the
    // Material branch rather than a converted ColorScheme. Feeding both engines identical
    // inputs is what keeps the two design systems looking consistent.
    val seed: Color? = when {
        state.colorSource == ColorSource.Custom -> Color(state.seedColor)
        state.colorSource == ColorSource.Wallpaper && supportsDynamic ->
            if (isDark) dynamicDarkColorScheme(context).primary
            else dynamicLightColorScheme(context).primary
        // null tells Miuix to use its own stock palette.
        else -> null
    }

    val mode = if (state.isSeeded) {
        when (state.darkMode) {
            DarkMode.FollowSystem -> ColorSchemeMode.MonetSystem
            DarkMode.Light -> ColorSchemeMode.MonetLight
            DarkMode.Dark -> ColorSchemeMode.MonetDark
        }
    } else {
        when (state.darkMode) {
            DarkMode.FollowSystem -> ColorSchemeMode.System
            DarkMode.Light -> ColorSchemeMode.Light
            DarkMode.Dark -> ColorSchemeMode.Dark
        }
    }

    val paletteStyle = remember(state.paletteStyle) {
        runCatching { MiuixPaletteStyle.valueOf(state.paletteStyle.name) }
            .getOrDefault(MiuixPaletteStyle.TonalSpot)
    }
    val colorSpec = when (state.effectiveColorSpec) {
        moe.shizuku.manager.theme.ColorSpec.Spec2025 -> MiuixColorSpec.Spec2025
        moe.shizuku.manager.theme.ColorSpec.Spec2021 -> MiuixColorSpec.Spec2021
    }

    val controller = remember(mode, seed, paletteStyle, colorSpec, isDark) {
        if (seed != null) {
            MiuixThemeController(
                colorSchemeMode = mode,
                keyColor = seed,
                paletteStyle = paletteStyle,
                colorSpec = colorSpec,
                isDark = isDark,
            )
        } else {
            MiuixThemeController(colorSchemeMode = mode, isDark = isDark)
        }
    }

    val navigationEventDispatcherOwner = rememberNavigationEventDispatcherOwner(parent = null)

    CompositionLocalProvider(
        LocalNavigationEventDispatcherOwner provides navigationEventDispatcherOwner
    ) {
        MiuixTheme(controller = controller, content = content)
    }
}

/**
 * Cross-fades every role so switching accent color or light/dark reads as a transition rather
 * than a jump.
 */
@Composable
private fun ColorScheme.animated(): ColorScheme {
    @Composable
    fun c(value: Color): Color =
        animateColorAsState(value, spring(), label = "themeColor").value

    return copy(
        primary = c(primary),
        onPrimary = c(onPrimary),
        primaryContainer = c(primaryContainer),
        onPrimaryContainer = c(onPrimaryContainer),
        inversePrimary = c(inversePrimary),
        secondary = c(secondary),
        onSecondary = c(onSecondary),
        secondaryContainer = c(secondaryContainer),
        onSecondaryContainer = c(onSecondaryContainer),
        tertiary = c(tertiary),
        onTertiary = c(onTertiary),
        tertiaryContainer = c(tertiaryContainer),
        onTertiaryContainer = c(onTertiaryContainer),
        background = c(background),
        onBackground = c(onBackground),
        surface = c(surface),
        onSurface = c(onSurface),
        surfaceVariant = c(surfaceVariant),
        onSurfaceVariant = c(onSurfaceVariant),
        surfaceTint = c(surfaceTint),
        inverseSurface = c(inverseSurface),
        inverseOnSurface = c(inverseOnSurface),
        error = c(error),
        onError = c(onError),
        errorContainer = c(errorContainer),
        onErrorContainer = c(onErrorContainer),
        outline = c(outline),
        outlineVariant = c(outlineVariant),
        scrim = c(scrim),
        surfaceBright = c(surfaceBright),
        surfaceDim = c(surfaceDim),
        surfaceContainer = c(surfaceContainer),
        surfaceContainerHigh = c(surfaceContainerHigh),
        surfaceContainerHighest = c(surfaceContainerHighest),
        surfaceContainerLow = c(surfaceContainerLow),
        surfaceContainerLowest = c(surfaceContainerLowest),
    )
}
