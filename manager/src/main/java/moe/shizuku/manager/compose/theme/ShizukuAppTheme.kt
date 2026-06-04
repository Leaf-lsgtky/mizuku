package moe.shizuku.manager.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import moe.shizuku.manager.ShizukuSettings

val LocalIsMiuix = staticCompositionLocalOf { false }

@Composable
fun ShizukuAppTheme(content: @Composable () -> Unit) {
    val isMiuix = ShizukuSettings.getThemeMode() == ShizukuSettings.THEME_MIUIX

    CompositionLocalProvider(LocalIsMiuix provides isMiuix) {
        if (isMiuix) {
            ShizukuMiuixTheme(content = content)
        } else {
            ShizukuTheme(content = content)
        }
    }
}
