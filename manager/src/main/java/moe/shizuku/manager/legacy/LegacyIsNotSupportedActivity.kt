package moe.shizuku.manager.legacy

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import moe.shizuku.manager.MainActivity
import moe.shizuku.manager.R
import moe.shizuku.manager.app.AppActivity
import moe.shizuku.manager.compose.theme.ShizukuTheme

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
            ShizukuTheme {
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.dialog_requesting_legacy_title, label))
        },
        text = {
            Text(
                text = stringResource(R.string.dialog_requesting_legacy_message, label),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onOpenShizuku()
                onDismiss()
            }) {
                Text(stringResource(R.string.dialog_requesting_legacy_button_open_shizuku))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        },
    )
}

@Composable
private fun LegacyNotSupportedDialog(
    label: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.dialog_legacy_not_support_title, label))
        },
        text = {
            Text(
                text = stringResource(R.string.dialog_legacy_not_support_message, label),
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
