package moe.shizuku.manager.settings

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import moe.shizuku.manager.R
import moe.shizuku.manager.app.AppActivity
import moe.shizuku.manager.compose.screens.AppearanceScreen
import moe.shizuku.manager.compose.theme.LocalIsMiuix
import moe.shizuku.manager.compose.theme.ProvideMiuixTheme
import moe.shizuku.manager.compose.theme.ShizukuAppTheme
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold

/**
 * Hosts the consolidated appearance settings. Renders with whichever design system is active.
 */
class AppearanceActivity : AppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
        )
        window.isNavigationBarContrastEnforced = false

        setContent {
            ShizukuAppTheme {
                if (LocalIsMiuix.current) {
                    MiuixAppearance(onBack = { finish() })
                } else {
                    MaterialAppearance(onBack = { finish() })
                }
            }
        }
    }
}

@Composable
private fun MiuixAppearance(onBack: () -> Unit) {
    val scrollBehavior = MiuixScrollBehavior()

    MiuixScaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.settings_user_interface),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { padding ->
        AppearanceScreen(
            scrollBehavior = scrollBehavior,
            scaffoldPadding = padding,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaterialAppearance(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_user_interface)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { padding ->
        // The appearance content itself is Miuix-built, so give it a matching Miuix theme.
        Box(Modifier.fillMaxSize().padding(padding)) {
            ProvideMiuixTheme {
                AppearanceScreen(scrollBehavior = MiuixScrollBehavior())
            }
        }
    }
}
