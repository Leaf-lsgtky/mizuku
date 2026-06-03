package moe.shizuku.manager.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun ShizukuMiuixTheme(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()

    val mode = if (isDark) ColorSchemeMode.Dark else ColorSchemeMode.Light
    val controller = remember(mode, isDark) {
        ThemeController(
            colorSchemeMode = mode,
            isDark = isDark
        )
    }

    MiuixTheme(controller = controller, content = content)
}
