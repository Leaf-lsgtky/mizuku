package moe.shizuku.manager.legacy

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import moe.shizuku.manager.MainActivity
import moe.shizuku.manager.R
import moe.shizuku.manager.ktx.toHtml
import moe.shizuku.manager.app.AppActivity
import moe.shizuku.manager.compose.theme.LocalIsMiuix
import moe.shizuku.manager.compose.theme.ShizukuAppTheme
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

class LegacyIsNotSupportedActivity : AppActivity() {

    companion object {
        private const val RESULT_ERROR = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callingComponent = callingActivity
        if (callingComponent == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        val ai = try {
            packageManager.getApplicationInfo(callingComponent.packageName, PackageManager.GET_META_DATA)
        } catch (e: Throwable) {
            finish()
            return
        }

        val label = try {
            ai.loadLabel(packageManager)
        } catch (e: Exception) {
            ai.packageName
        }

        val v3Support = ai.metaData?.getBoolean("moe.shizuku.client.V3_SUPPORT") == true

        setContent {
            ShizukuAppTheme {
                if (v3Support) {
                    LegacyV3SupportDialog(
                        label = label.toString(),
                        onDismiss = {
                            setResult(RESULT_ERROR)
                            finish()
                        },
                        onOpenShizuku = {
                            startActivity(
                                Intent(this@LegacyIsNotSupportedActivity, MainActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        },
                    )
                } else {
                    LegacyNotSupportedDialog(
                        label = label.toString(),
                        onDismiss = {
                            setResult(RESULT_ERROR)
                            finish()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LegacyV3SupportDialog(
    label: String,
    onDismiss: () -> Unit,
    onOpenShizuku: () -> Unit,
) {
    val title = stringResource(R.string.dialog_requesting_legacy_title, label)
    val openShizukuText = stringResource(R.string.dialog_requesting_legacy_button_open_shizuku)
    val okText = stringResource(android.R.string.ok)

    if (LocalIsMiuix.current) {
        val messagePlain = stringResource(R.string.dialog_requesting_legacy_message_plain, label)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            text = okText,
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            text = openShizukuText,
                            onClick = {
                                onOpenShizuku()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                        )
                    }
                }
            }
        )
    } else {
        val context = LocalContext.current
        val messageHtml = context.getString(R.string.dialog_requesting_legacy_message, label)
            .toHtml(rikka.html.text.HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                androidx.compose.material3.Text(text = title)
            },
            text = {
                AndroidView(
                    factory = { ctx ->
                        android.widget.TextView(ctx).apply {
                            text = messageHtml
                        }
                    },
                    update = { tv ->
                        tv.text = messageHtml
                    },
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    onOpenShizuku()
                    onDismiss()
                }) {
                    androidx.compose.material3.Text(openShizukuText)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    androidx.compose.material3.Text(okText)
                }
            },
        )
    }
}

@Composable
private fun LegacyNotSupportedDialog(
    label: String,
    onDismiss: () -> Unit,
) {
    val title = stringResource(R.string.dialog_legacy_not_support_title, label)
    val okText = stringResource(android.R.string.ok)

    if (LocalIsMiuix.current) {
        val messagePlain = stringResource(R.string.dialog_legacy_not_support_message_plain, label)
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
        val messageHtml = context.getString(R.string.dialog_legacy_not_support_message, label)
            .toHtml(rikka.html.text.HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                androidx.compose.material3.Text(text = title)
            },
            text = {
                AndroidView(
                    factory = { ctx ->
                        android.widget.TextView(ctx).apply {
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
