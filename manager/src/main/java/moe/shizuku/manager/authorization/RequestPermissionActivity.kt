package moe.shizuku.manager.authorization

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import moe.shizuku.manager.Helps
import moe.shizuku.manager.R
import moe.shizuku.manager.ktx.toHtml
import moe.shizuku.manager.app.AppActivity
import moe.shizuku.manager.compose.theme.LocalIsMiuix
import moe.shizuku.manager.compose.theme.ShizukuAppTheme
import moe.shizuku.manager.utils.Logger.LOGGER
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_ALLOWED
import rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_IS_ONETIME
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

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
            ShizukuAppTheme {
                var binderReady by remember { mutableStateOf(false) }
                var binderFailed by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    binderReady = waitForBinder()
                    if (!binderReady) {
                        binderFailed = true
                    }
                }

                if (binderFailed) {
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
    val allowText = stringResource(R.string.grant_dialog_button_allow_always)
    val denyText = stringResource(R.string.grant_dialog_button_deny)

    if (LocalIsMiuix.current) {
        val titlePlain = stringResource(R.string.permission_warning_template_plain, label, stringResource(R.string.permission_group_description))
        WindowDialog(
            show = true,
            onDismissRequest = {},
            title = titlePlain,
            content = {
                Column {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            text = denyText,
                            onClick = onDeny,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            text = allowText,
                            onClick = onAllow,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                        )
                    }
                }
            }
        )
    } else {
        val context = LocalContext.current
        val titleHtml = context.getString(R.string.permission_warning_template, label, stringResource(R.string.permission_group_description))
            .toHtml(rikka.html.text.HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
        AlertDialog(
            onDismissRequest = {},
            title = {
                AndroidView(
                    factory = { ctx ->
                        android.widget.TextView(ctx).apply {
                            text = titleHtml
                        }
                    },
                    update = { tv ->
                        tv.text = titleHtml
                    },
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = onAllow) {
                    androidx.compose.material3.Text(allowText)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = onDeny) {
                    androidx.compose.material3.Text(denyText)
                }
            },
        )
    }
}

@Composable
private fun AdbLimitedDialog(onDismiss: () -> Unit) {
    val title = "Shizuku: ${stringResource(R.string.app_management_dialog_adb_is_limited_title)}"
    val okText = stringResource(android.R.string.ok)

    if (LocalIsMiuix.current) {
        val messagePlain = stringResource(R.string.app_management_dialog_adb_is_limited_message_plain, Helps.ADB.get())
        WindowDialog(
            show = true,
            onDismissRequest = onDismiss,
            title = title,
            content = {
                Column {
                    Text(
                        text = messagePlain,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    TextButton(
                        text = okText,
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }
        )
    } else {
        val context = LocalContext.current
        val messageHtml = context.getString(R.string.app_management_dialog_adb_is_limited_message, Helps.ADB.get())
            .toHtml(rikka.html.text.HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                androidx.compose.material3.Text(title)
            },
            text = {
                AndroidView(
                    factory = { ctx ->
                        android.widget.TextView(ctx).apply {
                            movementMethod = android.text.method.LinkMovementMethod.getInstance()
                            text = messageHtml
                        }
                    },
                    update = { tv ->
                        tv.text = messageHtml
                    },
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    androidx.compose.material3.Text(okText)
                }
            },
        )
    }
}
