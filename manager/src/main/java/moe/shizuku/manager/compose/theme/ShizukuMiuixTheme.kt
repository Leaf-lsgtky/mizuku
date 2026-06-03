package moe.shizuku.manager.compose.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.colorResource
import moe.shizuku.manager.app.ThemeHelper
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun ShizukuMiuixTheme(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val useSystemColor = ThemeHelper.isUsingSystemColor()

    val controller = if (useSystemColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val keyColor = colorResource(id = android.R.color.system_accent1_500)
        val mode = if (isDark) ColorSchemeMode.MonetDark else ColorSchemeMode.MonetLight
        
        remember(mode, keyColor, isDark) {
            ThemeController(
                colorSchemeMode = mode,
                keyColor = keyColor,
                isDark = isDark
            )
        }
    } else {
        val mode = if (isDark) ColorSchemeMode.Dark else ColorSchemeMode.Light
        remember(mode, isDark) {
            ThemeController(
                colorSchemeMode = mode,
                isDark = isDark
            )
        }
    }

    MiuixTheme(controller = controller, content = content)
}
