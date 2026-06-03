package moe.shizuku.manager.settings

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.R
import moe.shizuku.manager.app.AppActivity
import moe.shizuku.manager.compose.theme.ShizukuTheme
import moe.shizuku.manager.utils.CustomTabsHelper
import moe.shizuku.manager.worker.AdbStartWorker

class BugReportDialogActivity : AppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ShizukuTheme {
                BugReportDialog(
                    onDismiss = { finish() },
                )
            }
        }
    }
}

@Composable
private fun BugReportDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = {
            cancelWorkerNotification(context)
            onDismiss()
        },
        title = { Text(stringResource(R.string.settings_report_bug)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(R.string.bug_report_dialog_before_reporting),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Update link
                val updateText = stringResource(R.string.bug_report_dialog_update)
                val updateLinkText = stringResource(R.string.bug_report_dialog_link_update)
                LinkText(
                    template = updateText,
                    linkText = updateLinkText,
                    url = "https://github.com/thedjchi/Shizuku/releases/latest",
                )

                // Wiki link
                val wikiText = stringResource(R.string.bug_report_dialog_wiki)
                val wikiLinkText = stringResource(R.string.bug_report_dialog_link_wiki)
                LinkText(
                    template = wikiText,
                    linkText = wikiLinkText,
                    url = "https://github.com/thedjchi/Shizuku/wiki#troubleshooting",
                )

                // Issues link
                val issuesText = stringResource(R.string.bug_report_dialog_issues)
                val issuesLinkText = stringResource(R.string.bug_report_dialog_link_issues)
                LinkText(
                    template = issuesText,
                    linkText = issuesLinkText,
                    url = "https://github.com/thedjchi/Shizuku/issues",
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // Method text
                val methodText = stringResource(R.string.bug_report_dialog_method)
                val methodLinkText = "GitHub"
                LinkText(
                    template = methodText,
                    linkText = methodLinkText,
                    url = "https://github.com/thedjchi/Shizuku/issues/new",
                    bold = true,
                )

                Text(
                    text = stringResource(R.string.bug_report_dialog_method_2),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                CustomTabsHelper.launchUrlOrCopy(context, "https://github.com/thedjchi/Shizuku/issues/new")
            }) {
                Text("GitHub")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                val plainBody = """
                    Please describe the bug. Include steps to reproduce if possible, as well as any relevant images/logs.

                    Device: ${Build.MANUFACTURER} ${Build.MODEL}
                    Android Version: ${Build.VERSION.RELEASE}
                    Shizuku Version: ${BuildConfig.VERSION_NAME}
                """.trimIndent()

                val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse(
                    "mailto:" + context.getString(R.string.support_email) +
                    "?subject=" + Uri.encode("[ISSUE TITLE]") +
                    "&body=" + Uri.encode(plainBody)
                ))
                try {
                    context.startActivity(emailIntent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, context.getString(R.string.toast_no_email_app), Toast.LENGTH_SHORT).show()
                }
            }) {
                Text(stringResource(R.string.bug_report_dialog_button_email))
            }
        },
    )
}

@Composable
private fun LinkText(
    template: String,
    linkText: String,
    url: String,
    bold: Boolean = false,
) {
    val context = LocalContext.current
    val annotatedString = buildAnnotatedString {
        val parts = template.split("^1")
        if (parts.size == 2) {
            append(parts[0])
            pushStringAnnotation(tag = "URL", annotation = url)
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = if (bold) androidx.compose.ui.text.font.FontWeight.Bold else null,
                )
            ) {
                append(linkText)
            }
            pop()
            append(parts[1])
        } else {
            append(template)
        }
    }

    androidx.compose.foundation.text.ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    CustomTabsHelper.launchUrlOrCopy(context, annotation.item)
                }
        },
    )
}

private fun cancelWorkerNotification(context: Context) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.cancel(AdbStartWorker.NOTIFICATION_ID)
}
