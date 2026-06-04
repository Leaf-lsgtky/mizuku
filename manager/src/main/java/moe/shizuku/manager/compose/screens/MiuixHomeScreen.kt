package moe.shizuku.manager.compose.screens

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.method.LinkMovementMethod
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.Helps
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.AdbKey
import moe.shizuku.manager.adb.AdbMdns
import moe.shizuku.manager.adb.AdbPairingClient
import moe.shizuku.manager.adb.PreferenceAdbKeyStore
import moe.shizuku.manager.ktx.toHtml
import moe.shizuku.manager.model.ServiceStatus
import moe.shizuku.manager.utils.CustomTabsHelper
import moe.shizuku.manager.utils.EnvironmentUtils
import moe.shizuku.manager.utils.SettingsHelper
import moe.shizuku.manager.utils.SettingsPage
import moe.shizuku.manager.utils.ShizukuStateMachine
import moe.shizuku.manager.utils.UserHandleCompat
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants
import rikka.html.text.HtmlCompat
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Help
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
fun MiuixHomeScreen(
    homeViewModel: moe.shizuku.manager.home.HomeViewModel,
    appsViewModel: moe.shizuku.manager.management.AppsViewModel,
    onNavigateToStarter: (isRoot: Boolean, port: Int) -> Unit,
    onNavigateToShellTutorial: () -> Unit,
    onNavigateToAdbPairingTutorial: () -> Unit,
    scrollBehavior: top.yukonga.miuix.kmp.basic.ScrollBehavior,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val serviceStatusResource by homeViewModel.serviceStatus.observeAsState()
    val grantedCountResource by appsViewModel.grantedCount.observeAsState()
    val shouldShowBatteryOpt by homeViewModel.shouldShowBatteryOptimizationSnackbar.observeAsState(false)

    val status = serviceStatusResource?.data ?: ServiceStatus()
    val grantedCount = grantedCountResource?.data ?: 0
    val isRunning = status.isRunning
    val isRoot = status.uid == 0
    val adbPermission = status.permission
    val isPrimaryUser = UserHandleCompat.myUserId() == 0

    var showAboutDialog by remember { mutableStateOf(false) }
    var showStopDialog by remember { mutableStateOf(false) }
    var showAutomationSheet by remember { mutableStateOf(false) }
    var showAdbCommandDialog by remember { mutableStateOf(false) }
    var showAdbDiscoveryDialog by remember { mutableStateOf(false) }
    var showAdbPairDialog by remember { mutableStateOf(false) }
    var showWadbNotEnabledDialog by remember { mutableStateOf(false) }
    var showUsbDebuggingNotEnabledDialog by remember { mutableStateOf(false) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        homeViewModel.checkBatteryOptimization()
    }

    LaunchedEffect(shouldShowBatteryOpt) {
        if (shouldShowBatteryOpt) {
            SettingsHelper.requestIgnoreBatteryOptimizations(context, null)
        }
    }

    if (showAboutDialog) {
        MiuixAboutDialog(onDismiss = { showAboutDialog = false })
    }
    if (showStopDialog) {
        MiuixStopShizukuDialog(
            onDismiss = { showStopDialog = false },
            onConfirm = {
                ShizukuStateMachine.set(ShizukuStateMachine.State.STOPPING)
                runCatching { Shizuku.exit() }
            },
        )
    }
    if (showAutomationSheet) {
        MiuixAutomationBottomSheet(onDismiss = { showAutomationSheet = false })
    }
    if (showAdbCommandDialog) {
        MiuixAdbCommandDialog(onDismiss = { showAdbCommandDialog = false })
    }
    if (showAdbDiscoveryDialog) {
        MiuixAdbDiscoveryDialog(
            onDismiss = { showAdbDiscoveryDialog = false },
            onPortDiscovered = { port ->
                showAdbDiscoveryDialog = false
                onNavigateToStarter(false, port)
            },
        )
    }
    if (showAdbPairDialog) {
        MiuixAdbPairDialog(
            onDismiss = { showAdbPairDialog = false },
            onPairSuccess = {
                showAdbPairDialog = false
                Toast.makeText(context, context.getString(R.string.notification_adb_pairing_succeed_title), Toast.LENGTH_SHORT).show()
            },
        )
    }
    if (showWadbNotEnabledDialog) {
        MiuixWadbNotEnabledDialog(onDismiss = { showWadbNotEnabledDialog = false })
    }
    if (showUsbDebuggingNotEnabledDialog) {
        MiuixUsbDebuggingNotEnabledDialog(onDismiss = { showUsbDebuggingNotEnabledDialog = false })
    }
    if (showAccessibilityDialog) {
        MiuixAccessibilityDialog(
            onDismiss = { showAccessibilityDialog = false },
            onNavigateToSettings = { page ->
                showAccessibilityDialog = false
                page.launch(context)
            },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .scrollEndHaptic()
            .overScrollVertical(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            MiuixServerStatusCard(
                status = status,
                grantedCount = grantedCount,
                onGrantedClick = { /* navigate to apps tab */ }
            )
        }

        if (adbPermission) {
            item {
                MiuixTerminalCard(onClick = onNavigateToShellTutorial)
            }
        }

        if (isRunning && !adbPermission) {
            item {
                MiuixAdbPermissionLimitedCard()
            }
        }

        if (isPrimaryUser) {
            val rootRestart = isRunning && isRoot
            if (EnvironmentUtils.isRooted()) {
                item {
                    MiuixStartRootCard(
                        isRestart = rootRestart,
                        onClick = { onNavigateToStarter(true, 0) },
                    )
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ||
                EnvironmentUtils.isTelevision() ||
                EnvironmentUtils.getAdbTcpPort() > 0
            ) {
                item {
                    MiuixStartWirelessAdbCard(
                        onStartWadb = {
                            StartWirelessAdbHelper.start(
                                context = context,
                                scope = scope,
                                onNavigateToStarter = onNavigateToStarter,
                                onShowWadbNotEnabled = { showWadbNotEnabledDialog = true },
                                onShowUsbDebuggingNotEnabled = { showUsbDebuggingNotEnabledDialog = true },
                            )
                        },
                        onPair = {
                            if (EnvironmentUtils.isTelevision()) {
                                showAccessibilityDialog = true
                            } else if (ShizukuSettings.getLegacyPairing()) {
                                showAdbPairDialog = true
                            } else {
                                onNavigateToAdbPairingTutorial()
                            }
                        },
                        onViewGuide = {
                            CustomTabsHelper.launchUrlOrCopy(context, Helps.ADB_ANDROID11.get())
                        },
                    )
                }
            }

            item {
                MiuixStartAdbCard(onClick = { showAdbCommandDialog = true })
            }
        }

        item {
            MiuixAutomationCard(onClick = { showAutomationSheet = true })
        }

        item {
            MiuixLearnMoreCard()
        }
    }
}

// --- Cards ---

@Composable
private fun MiuixServerStatusCard(
    status: ServiceStatus,
    grantedCount: Int,
    onGrantedClick: () -> Unit,
) {
    val isRunning = status.isRunning
    val isRoot = status.uid == 0
    val user = if (isRoot) "root" else "adb"

    val latestApiVersion = Shizuku.getLatestServiceVersion()
    val latestPatchVersion = ShizukuApiConstants.SERVER_PATCH_VERSION
    val hasUpdate = isRunning && (status.apiVersion != latestApiVersion || status.patchVersion != latestPatchVersion)

    val containerColor = if (isRunning) {
        when {
            MiuixTheme.isDynamicColor -> MiuixTheme.colorScheme.secondaryContainer
            isSystemInDarkTheme() -> Color(0xFF1A3825)
            else -> Color(0xFFDFFAE4)
        }
    } else {
        when {
            MiuixTheme.isDynamicColor -> MiuixTheme.colorScheme.errorContainer
            isSystemInDarkTheme() -> Color(0xFF381A1A)
            else -> Color(0xFFFAEEEE)
        }
    }

    val textContentColor = if (isRunning) {
        if (MiuixTheme.isDynamicColor) MiuixTheme.colorScheme.onSecondaryContainer else MiuixTheme.colorScheme.onSurface
    } else {
        if (MiuixTheme.isDynamicColor) MiuixTheme.colorScheme.onErrorContainer else MiuixTheme.colorScheme.onSurface
    }

    val descTextColor = textContentColor.copy(alpha = 0.8f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.defaultColors(color = containerColor),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(50.dp, 38.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Icon(
                        modifier = Modifier.size(170.dp),
                        imageVector = if (isRunning) Icons.Rounded.CheckCircleOutline else Icons.Rounded.ErrorOutline,
                        tint = if (isRunning) {
                            if (MiuixTheme.isDynamicColor) MiuixTheme.colorScheme.primary.copy(alpha = 0.8f) else Color(0xFF36D167)
                        } else {
                            if (MiuixTheme.isDynamicColor) MiuixTheme.colorScheme.error.copy(alpha = 0.8f) else Color(0xFFD13636)
                        },
                        contentDescription = null
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp)
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = if (isRunning) {
                            stringResource(R.string.home_status_service_is_running, stringResource(R.string.app_name))
                        } else {
                            stringResource(R.string.home_status_service_not_running, stringResource(R.string.app_name))
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textContentColor
                    )
                    Spacer(Modifier.height(2.dp))
                    if (isRunning) {
                        val versionText = if (hasUpdate) {
                            stringResource(
                                R.string.home_status_service_version_update,
                                user,
                                "${status.apiVersion}.${status.patchVersion}",
                                "${latestApiVersion}.${latestPatchVersion}",
                            )
                        } else {
                            stringResource(
                                R.string.home_status_service_version,
                                user,
                                "${status.apiVersion}.${status.patchVersion}",
                            )
                        }
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = versionText.replace("<br>", "\n").replace(Regex("<[^>]*>"), ""),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = descTextColor,
                        )
                    }
                    Spacer(Modifier.height(36.dp))
                }
            }
        }

        if (isRunning) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiuixStatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.home_app_management_title),
                    value = grantedCount.toString(),
                    onClick = onGrantedClick
                )
                MiuixStatCard(
                    modifier = Modifier.weight(1f),
                    title = "API",
                    value = "${status.apiVersion}.${status.patchVersion}",
                    onClick = {}
                )
            }
        }
    }
}

@Composable
private fun MiuixStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(16.dp),
        onClick = onClick,
        showIndication = true,
        pressFeedbackType = PressFeedbackType.Tilt
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }
    }
}



@Composable
private fun MiuixTerminalCard(onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        BasicComponent(
            title = stringResource(R.string.home_terminal_title),
            summary = stringResource(R.string.home_terminal_description),
            endActions = {
                Icon(
                    imageVector = MiuixIcons.Help,
                    tint = MiuixTheme.colorScheme.onSurface,
                    contentDescription = null,
                )
            },
            onClick = onClick,
        )
    }
}

@Composable
private fun MiuixStartRootCard(isRestart: Boolean, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                BasicComponent(
                    title = stringResource(R.string.home_root_title),
                    endActions = {
                        Icon(
                            imageVector = MiuixIcons.Ok,
                            tint = MiuixTheme.colorScheme.onSurface,
                            contentDescription = null,
                        )
                    },
                )
                AndroidView(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    factory = { ctx ->
                        TextView(ctx).apply {
                            movementMethod = LinkMovementMethod.getInstance()
                            text = ctx.getString(
                                R.string.home_root_description,
                                "<b><a href=\"${Helps.SUI.get()}\">Sui</a></b>",
                                "Sui"
                            ).toHtml(rikka.html.text.HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
                        }
                    },
                    update = { tv ->
                        tv.text = tv.context.getString(
                            R.string.home_root_description,
                            "<b><a href=\"${Helps.SUI.get()}\">Sui</a></b>",
                            "Sui"
                        ).toHtml(rikka.html.text.HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
                    },
                )
            }
        }
        Button(onClick = onClick) {
            Text(
                text = stringResource(
                    if (isRestart) R.string.home_root_button_restart
                    else R.string.home_root_button_start
                ),
            )
        }
    }
}

@Composable
private fun MiuixStartWirelessAdbCard(
    onStartWadb: () -> Unit,
    onPair: () -> Unit,
    onViewGuide: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            BasicComponent(
                title = stringResource(R.string.home_wireless_adb_title),
                summary = stringResource(R.string.home_wireless_adb_description)
                    .replace("<p>", "\n").replace(Regex("<[^>]*>"), ""),
                endActions = {
                    Icon(
                        imageVector = MiuixIcons.Info,
                        tint = MiuixTheme.colorScheme.onSurface,
                        contentDescription = null,
                    )
                },
            )
        }
        if (EnvironmentUtils.isTlsSupported()) {
            TextButton(
                text = stringResource(R.string.home_wireless_adb_view_guide_button),
                onClick = onViewGuide,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onStartWadb) {
                Text(stringResource(R.string.start))
            }
            if (EnvironmentUtils.isTlsSupported()) {
                TextButton(
                    text = stringResource(R.string.adb_pairing),
                    onClick = onPair,
                )
            }
        }
    }
}

@Composable
private fun MiuixStartAdbCard(onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            BasicComponent(
                title = stringResource(R.string.home_adb_title),
                summary = stringResource(R.string.home_adb_button_view_command),
                endActions = {
                    Icon(
                        imageVector = MiuixIcons.Help,
                        tint = MiuixTheme.colorScheme.onSurface,
                        contentDescription = null,
                    )
                },
                onClick = onClick,
            )
            AndroidView(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                factory = { ctx ->
                    TextView(ctx).apply {
                        movementMethod = LinkMovementMethod.getInstance()
                        text = ctx.getString(R.string.home_adb_description, Helps.ADB.get())
                            .toHtml(rikka.html.text.HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
                    }
                },
                update = { tv ->
                    tv.text = tv.context.getString(R.string.home_adb_description, Helps.ADB.get())
                        .toHtml(rikka.html.text.HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
                },
            )
        }
    }
}

@Composable
private fun MiuixAutomationCard(onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        BasicComponent(
            title = stringResource(R.string.home_automation_title),
            summary = stringResource(R.string.home_automation_description),
            endActions = {
                Icon(
                    imageVector = MiuixIcons.Help,
                    tint = MiuixTheme.colorScheme.onSurface,
                    contentDescription = null,
                )
            },
            onClick = onClick,
        )
    }
}

@Composable
private fun MiuixAdbPermissionLimitedCard() {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        BasicComponent(
            title = stringResource(R.string.home_adb_is_limited_title),
            summary = stringResource(R.string.home_adb_is_limited_description),
            endActions = {
                Icon(
                    imageVector = MiuixIcons.Info,
                    tint = MiuixTheme.colorScheme.onSurface,
                    contentDescription = null,
                )
            },
            onClick = { CustomTabsHelper.launchUrlOrCopy(context, Helps.ADB.get()) },
        )
    }
}

@Composable
private fun MiuixLearnMoreCard() {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        BasicComponent(
            title = stringResource(R.string.home_learn_more_title),
            summary = stringResource(R.string.home_learn_more_description),
            endActions = {
                Icon(
                    imageVector = MiuixIcons.Link,
                    tint = MiuixTheme.colorScheme.onSurface,
                    contentDescription = null,
                )
            },
            onClick = { CustomTabsHelper.launchUrlOrCopy(context, Helps.DEVELOPER.get()) },
        )
    }
}

// --- Dialogs ---

@Composable
private fun MiuixAboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName }

    WindowDialog(
        show = true,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.app_name),
    ) {
        Column {
            AndroidView(
                factory = { ctx ->
                    TextView(ctx).apply {
                        movementMethod = LinkMovementMethod.getInstance()
                        text = ctx.getString(
                            R.string.about_view_source_code,
                            "<b><a href=\"https://github.com/RikkaApps/Shizuku\">GitHub</a></b>"
                        ).toHtml()
                    }
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Version: $versionName",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                text = stringResource(R.string.about_dialog_button_close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MiuixStopShizukuDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    WindowDialog(
        show = true,
        onDismissRequest = onDismiss,
    ) {
        Column {
            Text(
                text = stringResource(R.string.dialog_stop_message),
                style = MiuixTheme.textStyles.body2,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    text = stringResource(android.R.string.ok),
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MiuixAdbCommandDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val command = "adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh"

    WindowDialog(
        show = true,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.home_adb_button_view_command),
    ) {
        Column {
            Text(
                text = stringResource(R.string.home_adb_dialog_view_command_message, command)
                    .replace(Regex("<[^>]*>"), ""),
                style = MiuixTheme.textStyles.body2,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    text = stringResource(R.string.home_adb_dialog_view_command_copy_button),
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("shizuku_command", command))
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                            Toast.makeText(context, context.getString(R.string.toast_copied_to_clipboard), Toast.LENGTH_SHORT).show()
                        }
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MiuixAutomationBottomSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var authToken by remember { mutableStateOf(ShizukuSettings.getAuthToken()) }
    var selectedAction by remember { mutableIntStateOf(0) }
    var showRegenerateDialog by remember { mutableStateOf(false) }
    val startAction = "${BuildConfig.APPLICATION_ID}.START"
    val stopAction = "${BuildConfig.APPLICATION_ID}.STOP"
    val currentAction = if (selectedAction == 0) startAction else stopAction

    WindowDialog(
        show = true,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.home_automation_bottom_sheet_intents),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                Text(
                    text = stringResource(R.string.home_automation_bottom_sheet_label_action),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selectedAction == 0,
                    onClick = { selectedAction = 0 },
                )
                Text(
                    text = startAction,
                    style = MiuixTheme.textStyles.body2,
                    modifier = Modifier
                        .clickable { selectedAction = 0 }
                        .weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selectedAction == 1,
                    onClick = { selectedAction = 1 },
                )
                Text(
                    text = stopAction,
                    style = MiuixTheme.textStyles.body2,
                    modifier = Modifier
                        .clickable { selectedAction = 1 }
                        .weight(1f),
                )
            }

            MiuixAutomationIntentRow(
                label = stringResource(R.string.home_automation_bottom_sheet_label_action),
                value = currentAction,
            )

            MiuixAutomationIntentRow(
                label = stringResource(R.string.home_automation_bottom_sheet_label_package),
                value = context.packageName,
            )

            MiuixAutomationIntentRow(
                label = stringResource(R.string.home_automation_bottom_sheet_label_target),
                value = "Broadcast Receiver",
            )

            Column {
                Text(
                    text = stringResource(R.string.home_automation_bottom_sheet_label_extras),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "auth_token=$authToken",
                        style = MiuixTheme.textStyles.body2,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("shizuku_auth_token", authToken))
                                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                                    Toast.makeText(context, context.getString(R.string.toast_copied_to_clipboard), Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 4.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(
                        onClick = {
                            showRegenerateDialog = true
                        },
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Refresh,
                            contentDescription = stringResource(R.string.home_automation_regenerate_token),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                text = stringResource(android.R.string.ok),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showRegenerateDialog) {
        WindowDialog(
            show = true,
            onDismissRequest = { showRegenerateDialog = false },
            title = stringResource(R.string.home_automation_regenerate_token),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.home_automation_regenerate_token_message),
                    style = MiuixTheme.textStyles.body2,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = { showRegenerateDialog = false },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        text = stringResource(android.R.string.ok),
                        onClick = {
                            authToken = ShizukuSettings.generateAuthToken()
                            showRegenerateDialog = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiuixAutomationIntentRow(label: String, value: String) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("shizuku_intent", value))
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                    Toast.makeText(context, context.getString(R.string.toast_copied_to_clipboard), Toast.LENGTH_SHORT).show()
                }
            }
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.body2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// --- ADB Discovery Dialog (mDNS) ---

@Composable
private fun MiuixAdbDiscoveryDialog(
    onDismiss: () -> Unit,
    onPortDiscovered: (Int) -> Unit,
) {
    val context = LocalContext.current
    var port by remember { mutableIntStateOf(-1) }
    val adbMdns = remember {
        AdbMdns(context, AdbMdns.TLS_CONNECT) { discoveredPort ->
            port = discoveredPort
        }
    }

    DisposableEffect(Unit) {
        adbMdns.start()
        if (context.checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
            Settings.Global.putInt(context.contentResolver, "adb_wifi_enabled", 1)
        }
        onDispose {
            adbMdns.stop()
        }
    }

    LaunchedEffect(port) {
        if (port in 1..65535) {
            onPortDiscovered(port)
        }
    }

    WindowDialog(
        show = true,
        onDismissRequest = {
            adbMdns.stop()
            onDismiss()
        },
        title = stringResource(R.string.dialog_adb_discovery),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.dialog_adb_discovery_message),
                style = MiuixTheme.textStyles.body2,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.dialog_adb_discovery_message_toggle_wireless_debugging),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = {
                        adbMdns.stop()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    text = stringResource(R.string.development_settings),
                    onClick = {
                        adbMdns.stop()
                        onDismiss()
                        SettingsPage.Developer.HighlightWirelessDebugging.launch(context)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// --- ADB Pair Dialog ---

@Composable
private fun MiuixAdbPairDialog(
    onDismiss: () -> Unit,
    onPairSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var port by remember { mutableStateOf("") }
    var pairingCode by remember { mutableStateOf("") }
    var isDiscovering by remember { mutableStateOf(true) }
    var portError by remember { mutableStateOf<String?>(null) }
    var codeError by remember { mutableStateOf<String?>(null) }
    var isPairing by remember { mutableStateOf(false) }
    var discoveredPort by remember { mutableIntStateOf(-1) }

    val adbMdns = remember {
        AdbMdns(context, AdbMdns.TLS_PAIRING) { discoveredPortValue ->
            discoveredPort = discoveredPortValue
        }
    }

    DisposableEffect(Unit) {
        adbMdns.start()
        onDispose {
            adbMdns.stop()
        }
    }

    LaunchedEffect(discoveredPort) {
        if (discoveredPort > 0) {
            port = discoveredPort.toString()
            isDiscovering = false
        }
    }

    val inMultiScreenOrDisplay = remember {
        val activity = context as? android.app.Activity
        activity?.isInMultiWindowMode == true ||
            (activity?.window?.decorView?.display?.displayId ?: -1) > 0
    }

    WindowDialog(
        show = true,
        onDismissRequest = {
            adbMdns.stop()
            onDismiss()
        },
        title = stringResource(
            if (isDiscovering && inMultiScreenOrDisplay) R.string.dialog_adb_pairing_discovery
            else R.string.dialog_adb_pairing_title
        ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isDiscovering && inMultiScreenOrDisplay) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
                Text(
                    text = stringResource(R.string.dialog_adb_pairing_message),
                    style = MiuixTheme.textStyles.body2,
                )
            } else {
                Text(
                    text = stringResource(R.string.dialog_adb_pairing_message),
                    style = MiuixTheme.textStyles.body2,
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.dialog_adb_port),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                top.yukonga.miuix.kmp.basic.TextField(
                    value = port,
                    onValueChange = {
                        port = it
                        portError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (portError != null) {
                    Text(
                        text = portError!!,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.error,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.dialog_adb_pairing_paring_code),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                top.yukonga.miuix.kmp.basic.TextField(
                    value = pairingCode,
                    onValueChange = {
                        pairingCode = it
                        codeError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (codeError != null) {
                    Text(
                        text = codeError!!,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.error,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = {
                        adbMdns.stop()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
                if (!isDiscovering || !inMultiScreenOrDisplay) {
                    TextButton(
                        text = if (isPairing) "" else stringResource(android.R.string.ok),
                        onClick = {
                            val portNum = port.toIntOrNull()
                            if (portNum == null || portNum !in 1..65535) {
                                portError = context.getString(R.string.dialog_adb_invalid_port)
                                return@TextButton
                            }
                            if (pairingCode.isBlank()) {
                                codeError = context.getString(R.string.paring_code_is_wrong)
                                return@TextButton
                            }

                            isPairing = true
                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    val key = AdbKey(PreferenceAdbKeyStore(ShizukuSettings.getPreferences()), "shizuku")
                                    val result = AdbPairingClient("127.0.0.1", portNum, pairingCode, key).runCatching {
                                        start()
                                    }.getOrNull()

                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        if (result == true) {
                                            adbMdns.stop()
                                            onPairSuccess()
                                        } else {
                                            codeError = context.getString(R.string.paring_code_is_wrong)
                                        }
                                        isPairing = false
                                    }
                                } catch (e: Exception) {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        when (e) {
                                            is moe.shizuku.manager.adb.AdbInvalidPairingCodeException -> {
                                                codeError = context.getString(R.string.paring_code_is_wrong)
                                            }
                                            is moe.shizuku.manager.adb.AdbKeyException -> {
                                                Toast.makeText(context, context.getString(R.string.adb_error_key_store), Toast.LENGTH_LONG).show()
                                            }
                                            else -> {
                                                portError = context.getString(R.string.cannot_connect_port)
                                            }
                                        }
                                        isPairing = false
                                    }
                                }
                            }
                        },
                        enabled = !isPairing,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// --- Wadb Not Enabled Dialog ---

@Composable
private fun MiuixWadbNotEnabledDialog(onDismiss: () -> Unit) {
    WindowDialog(
        show = true,
        onDismissRequest = onDismiss,
    ) {
        Column {
            Text(
                text = stringResource(R.string.dialog_wireless_adb_not_enabled),
                style = MiuixTheme.textStyles.body2,
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                text = stringResource(android.R.string.ok),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// --- USB Debugging Not Enabled Dialog ---

@Composable
private fun MiuixUsbDebuggingNotEnabledDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    WindowDialog(
        show = true,
        onDismissRequest = onDismiss,
    ) {
        Column {
            Text(
                text = stringResource(R.string.dialog_usb_debugging_not_enabled),
                style = MiuixTheme.textStyles.body2,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    text = stringResource(R.string.development_settings),
                    onClick = {
                        onDismiss()
                        SettingsPage.Developer.HighlightUsbDebugging.launch(context)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// --- Accessibility Dialog (for TV devices) ---

@Composable
private fun MiuixAccessibilityDialog(
    onDismiss: () -> Unit,
    onNavigateToSettings: (SettingsPage) -> Unit,
) {
    val context = LocalContext.current
    val hasWriteSecureSettings = remember {
        context.checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }
    val installer = remember { context.packageManager.getInstallerPackageName(context.packageName) }
    val isInstalledByPlayOrAdb = remember { installer == "com.android.vending" || installer == null }
    val hasAccessRestrictedSettings = remember {
        isInstalledByPlayOrAdb || Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    }
    val isAccessibilityEnabled = remember { isAccessibilityEnabled(context) }

    when {
        isAccessibilityEnabled -> {
            WindowDialog(
                show = true,
                onDismissRequest = onDismiss,
                title = stringResource(R.string.dialog_adb_pairing_title),
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.dialog_adb_pairing_accessibility_navigate),
                        style = MiuixTheme.textStyles.body2,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            text = stringResource(android.R.string.cancel),
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            text = stringResource(R.string.development_settings),
                            onClick = {
                                onDismiss()
                                SettingsPage.Developer.HighlightWirelessDebugging.launch(context)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        hasWriteSecureSettings -> {
            val enabled = remember { enableAccessibilityService(context) }
            if (enabled) {
                WindowDialog(
                    show = true,
                    onDismissRequest = onDismiss,
                    title = stringResource(R.string.dialog_adb_pairing_title),
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.dialog_adb_pairing_accessibility_navigate),
                            style = MiuixTheme.textStyles.body2,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TextButton(
                                text = stringResource(android.R.string.cancel),
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                text = stringResource(R.string.development_settings),
                                onClick = {
                                    onDismiss()
                                    SettingsPage.Developer.HighlightWirelessDebugging.launch(context)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                MiuixShowPermissionDialog(onDismiss = onDismiss, onContinue = {
                    onNavigateToSettings(SettingsPage.Accessibility)
                })
            }
        }
        !hasAccessRestrictedSettings -> {
            MiuixShowPermissionDialog(onDismiss = onDismiss, onContinue = {
                onNavigateToSettings(SettingsPage.Accessibility)
            })
        }
        else -> {
            WindowDialog(
                show = true,
                onDismissRequest = onDismiss,
                title = stringResource(R.string.dialog_adb_pairing_title),
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.dialog_adb_pairing_accessibility_enable),
                        style = MiuixTheme.textStyles.body2,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            text = stringResource(android.R.string.cancel),
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            text = stringResource(R.string.enable),
                            onClick = {
                                onDismiss()
                                SettingsPage.Accessibility.launch(context)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiuixShowPermissionDialog(
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
) {
    val context = LocalContext.current
    val permissionName = "ACCESS_RESTRICTED_SETTINGS"
    val permissionCommand = "adb shell cmd appops set ${context.packageName} $permissionName allow"

    WindowDialog(
        show = true,
        onDismissRequest = onDismiss,
        title = stringResource(android.R.string.dialog_alert_title),
    ) {
        Column {
            Text(
                text = stringResource(R.string.dialog_adb_pairing_accessibility_permission, permissionName, permissionCommand),
                style = MiuixTheme.textStyles.body2,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    text = "Continue",
                    onClick = {
                        onDismiss()
                        onContinue()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    val accessibilityServiceName = "${context.packageName}/${moe.shizuku.manager.adb.AdbPairingAccessibilityService::class.java.canonicalName}"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    )?.split(":")
    return enabledServices?.any { it.equals(accessibilityServiceName) } ?: false
}

private fun enableAccessibilityService(context: Context): Boolean {
    if (isAccessibilityEnabled(context)) return true

    val accessibilityServiceName = "${context.packageName}/${moe.shizuku.manager.adb.AdbPairingAccessibilityService::class.java.canonicalName}"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    )?.split(":")
    val newServices = if (enabledServices.isNullOrEmpty()) {
        accessibilityServiceName
    } else {
        enabledServices.joinToString(":") + ":$accessibilityServiceName"
    }

    Settings.Secure.putString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        newServices,
    )

    return isAccessibilityEnabled(context)
}
