package moe.shizuku.manager.compose.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.app.ThemeHelper
import rikka.core.res.resolveColor

private val LightColors = lightColorScheme(
    primary = Color(0xFF3F51B5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF001354),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFB1B8DF),
    onPrimary = Color(0xFF1E2B6B),
    primaryContainer = Color(0xFF354196),
    onPrimaryContainer = Color(0xFFDDE1FF),
)

private val BlackDarkColors = darkColorScheme(
    primary = Color(0xFFB1B8DF),
    onPrimary = Color(0xFF1E2B6B),
    primaryContainer = Color(0xFF354196),
    onPrimaryContainer = Color(0xFFDDE1FF),
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF1A1A1A),
    surfaceContainer = Color(0xFF111111),
    surfaceContainerLow = Color(0xFF0D0D0D),
    surfaceContainerHigh = Color(0xFF222222),
)

@Composable
fun ShizukuTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val useSystemColor = ThemeHelper.isUsingSystemColor()
    val isBlackNight = isDark && ThemeHelper.isBlackNightTheme(context)

    val colorScheme = when {
        useSystemColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        isBlackNight -> BlackDarkColors
        isDark -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
