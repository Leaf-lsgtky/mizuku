package moe.shizuku.manager.authorization

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import moe.shizuku.manager.Helps
import moe.shizuku.manager.R
import moe.shizuku.manager.app.AppActivity
import moe.shizuku.manager.compose.theme.ShizukuTheme
import moe.shizuku.manager.utils.Logger.LOGGER
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_ALLOWED
import rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_IS_ONETIME

class RequestPermissionActivity : AppActivity() {

    private fun dispatchResult(requestUid: Int, requestPid: Int, requestCode: Int, allowed: Boolean, onetime: Boolean) {
        val data = Bundle()
        data.putBoolean(REQUEST_PERMISSION_REPLY_ALLOWED, allowed)
        data.putBoolean(REQUEST_PERMISSION_REPLY_IS_ONETIME, onetime)
        try {
            Shizuku.dispatchPermissionConfirmationResult(requestUid, requestPid, requestCode, data)
        } catch (e: Throwable) {
            LOGGER.e("dispatchPermissionConfirmationResult")
        }
    }

    private suspend fun waitForBinder(): Boolean {
        return try {
            withTimeout(5000) {
                ShizukuStateMachine.asFlow().first { it == ShizukuStateMachine.State.RUNNING }
            }
            true
        } catch (e: TimeoutCancellationException) {
            LOGGER.e(e, "Binder not received in 5s")
            false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uid = intent.getIntExtra("uid", -1)
        val pid = intent.getIntExtra("pid", -1)
        val requestCode = intent.getIntExtra("requestCode", -1)
        val ai = intent.getParcelableExtra<android.content.pm.ApplicationInfo>("applicationInfo")
        if (uid == -1 || pid == -1 || ai == null) {
            finish()
            return
        }

        val hasPermission = Shizuku.checkRemotePermission("android.permission.GRANT_RUNTIME_PERMISSIONS") == PackageManager.PERMISSION_GRANTED

        val label = try {
            ai.loadLabel(packageManager)
        } catch (e: Exception) {
            ai.packageName
        }

        setContent {
            ShizukuTheme {
                var binderReady by remember { mutableStateOf(false) }
                var binderFailed by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    binderReady = waitForBinder()
                    if (!binderReady) {
                        binderFailed = true
                    }
                }

                if (binderFailed) {
                    // Binder not available, finish
                    LaunchedEffect(Unit) { finish() }
                } else if (binderReady) {
                    if (hasPermission) {
                        PermissionConfirmDialog(
                            label = label.toString(),
                            onAllow = {
                                dispatchResult(uid, pid, requestCode, allowed = true, onetime = false)
                                finish()
                            },
                            onDeny = {
                                dispatchResult(uid, pid, requestCode, allowed = false, onetime = true)
                                finish()
                            },
                        )
                    } else {
                        AdbLimitedDialog(
                            onDismiss = {
                                dispatchResult(uid, pid, requestCode, allowed = false, onetime = true)
                                finish()
                            },
                        )
                    }
                }
                // While waiting for binder, show nothing (activity window is transparent)
            }
        }
    }
}

@Composable
private fun PermissionConfirmDialog(
    label: String,
    onAllow: () -> Unit,
    onDeny: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = stringResource(R.string.permission_warning_template, label, stringResource(R.string.permission_group_description)),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onAllow) {
                Text(stringResource(R.string.grant_dialog_button_allow_always))
            }
        },
        dismissButton = {
            TextButton(onClick = onDeny) {
                Text(stringResource(R.string.grant_dialog_button_deny))
            }
        },
    )
}

@Composable
private fun AdbLimitedDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Shizuku: ${stringResource(R.string.app_management_dialog_adb_is_limited_title)}")
        },
        text = {
            Text(
                text = stringResource(R.string.app_management_dialog_adb_is_limited_message, Helps.ADB.get()),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        },
    )
}
