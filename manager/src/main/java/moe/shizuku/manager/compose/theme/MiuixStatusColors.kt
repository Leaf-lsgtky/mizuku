package moe.shizuku.manager.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Immutable
data class ServiceStatusPalette(
    val container: Color,
    val content: Color,
    val symbol: Color,
)

/** Service semantics remain recognisable when wallpaper colours change. */
@Composable
fun serviceStatusPalette(running: Boolean, pending: Boolean): ServiceStatusPalette {
    val colors = MiuixTheme.colorScheme
    val dark = LocalIsDark.current
    val container = when {
        pending -> if (dark) Color(0xFF3A3420) else Color(0xFFFFF8E1)
        running && MiuixTheme.isDynamicColor -> colors.secondaryContainer
        running -> if (dark) Color(0xFF1A3825) else Color(0xFFDFFAE4)
        else -> if (dark) Color(0xFF381A1A) else Color(0xFFFAEEEE)
    }
    val symbol = when {
        pending -> if (dark) Color(0xFFF9A825) else Color(0xFFFFB300)
        running && MiuixTheme.isDynamicColor -> colors.primary
        running -> if (dark) Color(0xFF81C784) else Color(0xFF36B85D)
        else -> if (dark) Color(0xFFEF9A9A) else Color(0xFFD13636)
    }
    return ServiceStatusPalette(
        container = container,
        content = if (running && MiuixTheme.isDynamicColor) colors.onSecondaryContainer else colors.onSurface,
        symbol = symbol,
    )
}
