package moe.shizuku.manager.shell

import android.os.Bundle
import androidx.activity.compose.setContent
import moe.shizuku.manager.app.AppActivity
import moe.shizuku.manager.compose.screens.ShellTutorialScreen
import moe.shizuku.manager.compose.theme.ShizukuAppTheme

class ShellTutorialActivity : AppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ShizukuAppTheme {
                ShellTutorialScreen(
                    onNavigateBack = { finish() },
                )
            }
        }
    }
}
