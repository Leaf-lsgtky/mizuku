package moe.shizuku.manager.compose.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import moe.shizuku.manager.app.ThemeHelper
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun ShizukuMiuixTheme(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val useSystemColor = ThemeHelper.isUsingSystemColor()

    val mode = when {
        useSystemColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) ColorSchemeMode.MonetDark else ColorSchemeMode.MonetLight
        }
        isDark -> ColorSchemeMode.Dark
        else -> ColorSchemeMode.Light
    }

    val controller = remember { ThemeController(mode) }
    MiuixTheme(controller = controller, content = content)
}
