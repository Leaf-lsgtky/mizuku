package moe.shizuku.manager.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import moe.shizuku.manager.app.AppActivity
import moe.shizuku.manager.compose.screens.SettingsScreen
import moe.shizuku.manager.compose.theme.ShizukuAppTheme

class SettingsActivity : AppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ShizukuAppTheme {
                SettingsScreen(
                    onNavigateBack = { finish() },
                )
            }
        }
    }
}
