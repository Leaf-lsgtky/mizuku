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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.R
import moe.shizuku.manager.app.AppActivity
import moe.shizuku.manager.compose.theme.LocalIsMiuix
import moe.shizuku.manager.compose.theme.ShizukuAppTheme
import moe.shizuku.manager.utils.CustomTabsHelper
import moe.shizuku.manager.worker.AdbStartWorker
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

class BugReportDialogActivity : AppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ShizukuAppTheme {
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
    val title = stringResource(R.string.settings_report_bug)
    val githubText = "GitHub"
    val emailText = stringResource(R.string.bug_report_dialog_button_email)

    val onDismissAction = {
        cancelWorkerNotification(context)
        onDismiss()
    }

    if (LocalIsMiuix.current) {
        WindowDialog(
            show = true,
            onDismissRequest = onDismissAction,
            title = title,
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = stringResource(R.string.bug_report_dialog_before_reporting),
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val updateText = stringResource(R.string.bug_report_dialog_update)
                    val updateLinkText = stringResource(R.string.bug_report_dialog_link_update)
                    MiuixLinkText(
                        template = updateText,
                        linkText = updateLinkText,
                        url = "https://github.com/thedjchi/Shizuku/releases/latest",
                    )

                    val wikiText = stringResource(R.string.bug_report_dialog_wiki)
                    val wikiLinkText = stringResource(R.string.bug_report_dialog_link_wiki)
                    MiuixLinkText(
                        template = wikiText,
                        linkText = wikiLinkText,
                        url = "https://github.com/thedjchi/Shizuku/wiki#troubleshooting",
                    )

                    val issuesText = stringResource(R.string.bug_report_dialog_issues)
                    val issuesLinkText = stringResource(R.string.bug_report_dialog_link_issues)
                    MiuixLinkText(
                        template = issuesText,
                        linkText = issuesLinkText,
                        url = "https://github.com/thedjchi/Shizuku/issues",
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val methodText = stringResource(R.string.bug_report_dialog_method)
                    MiuixLinkText(
                        template = methodText,
                        linkText = githubText,
                        url = "https://github.com/thedjchi/Shizuku/issues/new",
                        bold = true,
                    )

                    Text(
                        text = stringResource(R.string.bug_report_dialog_method_2),
                        color = MiuixTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            text = emailText,
                            onClick = {
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
                            },
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            text = githubText,
                            onClick = {
                                CustomTabsHelper.launchUrlOrCopy(context, "https://github.com/thedjchi/Shizuku/issues/new")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                        )
                    }
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismissAction,
            title = { androidx.compose.material3.Text(title) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    androidx.compose.material3.Text(
                        text = stringResource(R.string.bug_report_dialog_before_reporting),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val updateText = stringResource(R.string.bug_report_dialog_update)
                    val updateLinkText = stringResource(R.string.bug_report_dialog_link_update)
                    MaterialLinkText(
                        template = updateText,
                        linkText = updateLinkText,
                        url = "https://github.com/thedjchi/Shizuku/releases/latest",
                    )

                    val wikiText = stringResource(R.string.bug_report_dialog_wiki)
                    val wikiLinkText = stringResource(R.string.bug_report_dialog_link_wiki)
                    MaterialLinkText(
                        template = wikiText,
                        linkText = wikiLinkText,
                        url = "https://github.com/thedjchi/Shizuku/wiki#troubleshooting",
                    )

                    val issuesText = stringResource(R.string.bug_report_dialog_issues)
                    val issuesLinkText = stringResource(R.string.bug_report_dialog_link_issues)
                    MaterialLinkText(
                        template = issuesText,
                        linkText = issuesLinkText,
                        url = "https://github.com/thedjchi/Shizuku/issues",
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    val methodText = stringResource(R.string.bug_report_dialog_method)
                    MaterialLinkText(
                        template = methodText,
                        linkText = githubText,
                        url = "https://github.com/thedjchi/Shizuku/issues/new",
                        bold = true,
                    )

                    androidx.compose.material3.Text(
                        text = stringResource(R.string.bug_report_dialog_method_2),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    CustomTabsHelper.launchUrlOrCopy(context, "https://github.com/thedjchi/Shizuku/issues/new")
                }) {
                    androidx.compose.material3.Text(githubText)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
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
                    androidx.compose.material3.Text(emailText)
                }
            },
        )
    }
}

@Composable
private fun MiuixLinkText(
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
                    color = MiuixTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = if (bold) FontWeight.Bold else null,
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
        style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.body2.copy(
            color = MiuixTheme.colorScheme.onSurface,
        ),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    CustomTabsHelper.launchUrlOrCopy(context, annotation.item)
                }
        },
    )
}

@Composable
private fun MaterialLinkText(
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
                    fontWeight = if (bold) FontWeight.Bold else null,
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
