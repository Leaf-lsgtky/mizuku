package moe.shizuku.manager.management

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import moe.shizuku.manager.app.AppActivity
import moe.shizuku.manager.compose.screens.AppManagementScreen
import moe.shizuku.manager.compose.theme.ShizukuAppTheme
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.lifecycle.Status
import java.util.Objects

class ApplicationManagementActivity : AppActivity() {

    private val viewModel: AppsViewModel by viewModels()

    private val stateListener: (ShizukuStateMachine.State) -> Unit = {
        if (ShizukuStateMachine.isDead() && !isFinishing)
            finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ShizukuStateMachine.isRunning()) {
            finish()
            return
        }

        setContent {
            ShizukuAppTheme {
                AppManagementScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                )
            }
        }

        ShizukuStateMachine.addListener(stateListener)
    }

    override fun onDestroy() {
        ShizukuStateMachine.removeListener(stateListener)
        super.onDestroy()
    }
}
