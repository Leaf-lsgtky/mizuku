package moe.shizuku.manager.home

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import moe.shizuku.manager.adb.AdbPairingService
import moe.shizuku.manager.adb.AdbStarter
import moe.shizuku.manager.app.AppActivity
import moe.shizuku.manager.compose.screens.HomeScreen
import moe.shizuku.manager.compose.screens.MiuixMainScreen
import moe.shizuku.manager.compose.theme.LocalIsMiuix
import moe.shizuku.manager.compose.theme.ShizukuAppTheme
import moe.shizuku.manager.management.AppsViewModel
import moe.shizuku.manager.receiver.NotifCancelReceiver
import moe.shizuku.manager.settings.SettingsActivity
import moe.shizuku.manager.starter.StarterActivity
import moe.shizuku.manager.utils.EnvironmentUtils
import moe.shizuku.manager.utils.ShizukuStateMachine

open class HomeActivity : AppActivity() {

    private val homeModel: HomeViewModel by viewModels()
    private val appsModel: AppsViewModel by viewModels()

    private val stateListener: (ShizukuStateMachine.State) -> Unit = {
        if (ShizukuStateMachine.isRunning()) {
            homeModel.reload()
            appsModel.load()
        } else if (ShizukuStateMachine.isDead()) {
            homeModel.reload()
        }
    }

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
                    MiuixMainScreen(
                        homeViewModel = homeModel,
                        appsViewModel = appsModel,
                        onNavigateToStarter = { isRoot, port ->
                            startActivity(Intent(this, StarterActivity::class.java).apply {
                                putExtra(StarterActivity.EXTRA_IS_ROOT, isRoot)
                                putExtra(StarterActivity.EXTRA_PORT, port)
                            })
                        },
                        onNavigateToShellTutorial = {
                            startActivity(Intent(this, moe.shizuku.manager.shell.ShellTutorialActivity::class.java))
                        },
                        onNavigateToAdbPairingTutorial = {
                            startActivity(Intent(this, moe.shizuku.manager.adb.AdbPairingTutorialActivity::class.java))
                        },
                    )
                } else {
                    HomeScreen(
                        homeViewModel = homeModel,
                        appsViewModel = appsModel,
                        onNavigateToSettings = {
                            startActivity(Intent(this, SettingsActivity::class.java))
                        },
                        onNavigateToAppManagement = {
                            startActivity(Intent(this, moe.shizuku.manager.management.ApplicationManagementActivity::class.java))
                        },
                        onNavigateToStarter = { isRoot, port ->
                            startActivity(Intent(this, StarterActivity::class.java).apply {
                                putExtra(StarterActivity.EXTRA_IS_ROOT, isRoot)
                                putExtra(StarterActivity.EXTRA_PORT, port)
                            })
                        },
                        onNavigateToShellTutorial = {
                            startActivity(Intent(this, moe.shizuku.manager.shell.ShellTutorialActivity::class.java))
                        },
                        onNavigateToAdbPairingTutorial = {
                            startActivity(Intent(this, moe.shizuku.manager.adb.AdbPairingTutorialActivity::class.java))
                        },
                    )
                }
            }
        }

        ShizukuStateMachine.addListener(stateListener)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.let {
            val showDialog = it.getBooleanExtra(EXTRA_SHOW_PAIRING_DIALOG, false)
            if (showDialog) showAccessibilityDialog()

            val startWadb = it.getBooleanExtra(EXTRA_START_SERVICE_VIA_WADB, false)
            if (startWadb) {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(AdbPairingService.NOTIFICATION_ID)
                sendBroadcast(Intent(this, NotifCancelReceiver::class.java))
                startWirelessAdb()
            }
        }
    }

    private fun startWirelessAdb() {
        if (ShizukuStateMachine.get() == ShizukuStateMachine.State.STARTING) return

        val cr = contentResolver
        if (checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            android.provider.Settings.Global.putInt(cr, android.provider.Settings.Global.ADB_ENABLED, 1)
            android.provider.Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)
        }

        val adbEnabled = android.provider.Settings.Global.getInt(cr, android.provider.Settings.Global.ADB_ENABLED, 0)
        if (adbEnabled == 0) return

        val tcpPort = EnvironmentUtils.getAdbTcpPort()
        val tcpMode = moe.shizuku.manager.ShizukuSettings.getTcpMode()

        if (tcpPort <= 0 && !EnvironmentUtils.isTlsSupported()) {
            return
        } else if (tcpPort <= 0) {
            startActivity(Intent(this, StarterActivity::class.java))
        } else if (!tcpMode) {
            lifecycleScope.launch { AdbStarter.stopTcp(this@HomeActivity, tcpPort) }
            startActivity(Intent(this, StarterActivity::class.java))
        } else {
            startActivity(Intent(this, StarterActivity::class.java).apply {
                putExtra(StarterActivity.EXTRA_PORT, tcpPort)
            })
        }
    }

    override fun onResume() {
        super.onResume()
        homeModel.reload()
        appsModel.load()
    }

    override fun onDestroy() {
        ShizukuStateMachine.removeListener(stateListener)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_SHOW_PAIRING_DIALOG = "show_pairing_dialog"
        const val EXTRA_START_SERVICE_VIA_WADB = "start_service_via_wadb"
    }
}
