package moe.shizuku.manager.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
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
    val navigationEventDispatcherOwner = rememberNavigationEventDispatcherOwner(parent = null)

    CompositionLocalProvider(
        LocalNavigationEventDispatcherOwner provides navigationEventDispatcherOwner
    ) {
        MiuixTheme(controller = controller, content = content)
    }
}
