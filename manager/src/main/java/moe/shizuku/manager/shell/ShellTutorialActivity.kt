package moe.shizuku.manager.shell

import android.os.Bundle
import androidx.activity.compose.setContent
import moe.shizuku.manager.app.AppActivity
import moe.shizuku.manager.compose.screens.ShellTutorialScreen
import moe.shizuku.manager.compose.theme.ShizukuTheme

class ShellTutorialActivity : AppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ShizukuTheme {
                ShellTutorialScreen(
                    onNavigateBack = { finish() },
                )
            }
        }
    }
}
