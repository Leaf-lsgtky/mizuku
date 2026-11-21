package moe.shizuku.manager.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import moe.shizuku.manager.app.AppActivity
import moe.shizuku.manager.compose.screens.SettingsScreen
import moe.shizuku.manager.compose.screens.MiuixSettingsScreen
import moe.shizuku.manager.compose.components.miuix.Os4Scaffold
import moe.shizuku.manager.compose.theme.LocalIsMiuix
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import moe.shizuku.manager.R
import moe.shizuku.manager.compose.theme.ShizukuAppTheme

class SettingsActivity : AppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ShizukuAppTheme {
                val openAppearance = { startActivity(Intent(this, AppearanceActivity::class.java)) }
                if (LocalIsMiuix.current) {
                    Os4Scaffold(title = stringResource(R.string.settings_title), onBack = { finish() }) { padding, behavior, contentModifier ->
                        Box(Modifier.fillMaxSize().then(contentModifier)) {
                            MiuixSettingsScreen(
                                scrollBehavior = behavior,
                                scaffoldPadding = padding,
                                onNavigateToAppearance = openAppearance,
                            )
                        }
                    }
                } else {
                    SettingsScreen(onNavigateBack = { finish() }, onNavigateToAppearance = openAppearance)
                }
            }
        }
    }
}
