package moe.shizuku.manager.adb

import android.app.NotificationManager
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import moe.shizuku.manager.AppConstants
import moe.shizuku.manager.app.AppActivity
import moe.shizuku.manager.compose.screens.AdbPairingTutorialScreen
import moe.shizuku.manager.compose.theme.ShizukuAppTheme

class AdbPairingTutorialActivity : AppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isNotificationEnabled()) {
            startPairingService()
        }

        setContent {
            ShizukuAppTheme {
                AdbPairingTutorialScreen(
                    onNavigateBack = { finish() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isNotificationEnabled()) {
            startPairingService()
        }
    }

    private fun isNotificationEnabled(): Boolean {
        val nm = getSystemService(NotificationManager::class.java)
        val channel = nm.getNotificationChannel(AdbPairingService.NOTIFICATION_CHANNEL)
        return nm.areNotificationsEnabled() &&
                (channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE)
    }

    private fun startPairingService() {
        try {
            startForegroundService(AdbPairingService.startIntent(this))
        } catch (e: Throwable) {
            Log.e(AppConstants.TAG, "startForegroundService", e)
            try {
                startService(AdbPairingService.startIntent(this))
            } catch (_: Throwable) {
            }
        }
    }
}
