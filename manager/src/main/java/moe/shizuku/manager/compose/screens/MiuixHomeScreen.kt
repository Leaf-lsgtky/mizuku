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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.Helps
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.AdbKey
import moe.shizuku.manager.adb.AdbMdns
import moe.shizuku.manager.adb.AdbPairingClient
import moe.shizuku.manager.adb.AdbStarter
import moe.shizuku.manager.adb.PreferenceAdbKeyStore
import moe.shizuku.manager.ktx.toHtml
import moe.shizuku.manager.model.ServiceStatus
import moe.shizuku.manager.utils.CustomTabsHelper
import moe.shizuku.manager.utils.EnvironmentUtils
import moe.shizuku.manager.utils.SettingsHelper
import moe.shizuku.manager.utils.SettingsPage
import moe.shizuku.manager.utils.ShizukuStateMachine
import moe.shizuku.manager.utils.UserHandleCompat
import rikka.html.text.HtmlCompat
import rikka.lifecycle.Status
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.OutlinedTextField
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Apps
import top.yukonga.miuix.kmp.icon.extended.Code
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Devices
import top.yukonga.miuix.kmp.icon.extended.Help
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Security
import top.yukonga.miuix.kmp.icon.extended.Terminal
import top.yukonga.miuix.kmp.icon.extended.Warning
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun MiuixHomeScreen(
    homeViewModel: moe.shizuku.manager.home.HomeViewModel,
    appsViewModel: moe.shizuku.manager.management.AppsViewModel,
    onNavigateToStarter: (isRoot: Boolean, port: Int) -> Unit,
    onNavigateToShellTutorial: () -> Unit,
    onNavigateToAdbPairingTutorial: () -> Unit,
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
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            MiuixServerStatusCard(status = status)
        }

        if (adbPermission) {
            item {
                MiuixManageAppsCard(
                    grantedCount = grantedCount,
                )
            }
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

// --- Helper object for wireless ADB start ---
private object StartWirelessAdbHelper {
    fun start(
        context: Context,
        scope: kotlinx.coroutines.CoroutineScope,
        onNavigateToStarter: (Boolean, Int) -> Unit,
        onShowWadbNotEnabled: () -> Unit = {},
        onShowUsbDebuggingNotEnabled: () -> Unit = {},
    ) {
        if (ShizukuStateMachine.get() == ShizukuStateMachine.State.STARTING) {
            Toast.makeText(context, context.getString(R.string.toast_shizuku_already_starting), Toast.LENGTH_SHORT).show()
            return
        }
        val cr = context.contentResolver
        if (context.checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
            Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
            Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)
        }
        val adbEnabled = Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0)
        if (adbEnabled == 0) {
            onShowUsbDebuggingNotEnabled()
            return
        }
        val tcpPort = EnvironmentUtils.getAdbTcpPort()
        val tcpMode = ShizukuSettings.getTcpMode()
        if (tcpPort <= 0 && !EnvironmentUtils.isTlsSupported()) {
            onShowWadbNotEnabled()
        } else if (tcpPort <= 0) {
            onNavigateToStarter(false, 0)
        } else if (!tcpMode) {
            scope.launch { AdbStarter.stopTcp(context, tcpPort) }
            onNavigateToStarter(false, 0)
        } else {
            onNavigateToStarter(false, tcpPort)
        }
    }
}

// --- Cards ---

@Composable
private fun MiuixServerStatusCard(status: ServiceStatus) {
    val isRunning = status.isRunning
    val isRoot = status.uid == 0
    val user = if (isRoot) "root" else "adb"

    val latestApiVersion = Shizuku.getLatestServiceVersion()
    val latestPatchVersion = ShizukuApiConstants.SERVER_PATCH_VERSION
    val hasUpdate = isRunning && (status.apiVersion != latestApiVersion || status.patchVersion != latestPatchVersion)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = if (isRunning) MiuixTheme.colorScheme.primaryContainer
            else MiuixTheme.colorScheme.errorContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isRunning) MiuixIcons.Ok else MiuixIcons.Warning,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                tint = if (isRunning) MiuixTheme.colorScheme.onPrimaryContainer
                else MiuixTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                val title = if (isRunning) {
                    stringResource(R.string.home_status_service_is_running, stringResource(R.string.app_name))
                } else {
                    stringResource(R.string.home_status_service_not_running, stringResource(R.string.app_name))
                }
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.title2,
                    fontWeight = FontWeight.Bold,
                )
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
                        text = versionText,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiuixManageAppsCard(grantedCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = MiuixIcons.Apps,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                tint = MiuixTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_app_management_title),
                    style = MiuixTheme.textStyles.title2,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = pluralStringResource(R.plurals.home_app_management_authorized_apps_count, grantedCount, grantedCount),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

@Composable
private fun MiuixTerminalCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = MiuixIcons.Terminal,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                tint = MiuixTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(R.string.home_terminal_title),
                    style = MiuixTheme.textStyles.title2,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.home_terminal_description),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MiuixStartRootCard(isRestart: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Icon(
                imageVector = MiuixIcons.Security,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                tint = MiuixTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.home_root_title),
                style = MiuixTheme.textStyles.title2,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            val context = LocalContext.current
            AndroidView(
                factory = { ctx ->
                    TextView(ctx).apply {
                        movementMethod = LinkMovementMethod.getInstance()
                        text = ctx.getString(
                            R.string.home_root_description,
                            "<b><a href=\"${Helps.SUI.get()}\">Sui</a></b>",
                            "Sui"
                        ).toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
                    }
                },
                update = { tv ->
                    tv.text = tv.context.getString(
                        R.string.home_root_description,
                        "<b><a href=\"${Helps.SUI.get()}\">Sui</a></b>",
                        "Sui"
                    ).toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
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
}

@Composable
private fun MiuixStartWirelessAdbCard(
    onStartWadb: () -> Unit,
    onPair: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Icon(
                imageVector = MiuixIcons.Devices,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                tint = MiuixTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.home_wireless_adb_title),
                style = MiuixTheme.textStyles.title2,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_wireless_adb_description),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStartWadb) {
                    Text(stringResource(R.string.start))
                }
                if (EnvironmentUtils.isTlsSupported()) {
                    TextButton(
                        text = stringResource(R.string.adb_pairing),
                        onClick = onPair
                    )
                }
            }
        }
    }
}

@Composable
private fun MiuixStartAdbCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = MiuixIcons.Terminal,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    tint = MiuixTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.home_adb_title),
                        style = MiuixTheme.textStyles.title2,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.home_adb_button_view_command),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            val context = LocalContext.current
            AndroidView(
                factory = { ctx ->
                    TextView(ctx).apply {
                        movementMethod = LinkMovementMethod.getInstance()
                        text = ctx.getString(R.string.home_adb_description, Helps.ADB.get())
                            .toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
                    }
                },
                update = { tv ->
                    tv.text = tv.context.getString(R.string.home_adb_description, Helps.ADB.get())
                        .toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onClick) {
                Text(stringResource(R.string.home_adb_button_view_command))
            }
        }
    }
}

@Composable
private fun MiuixAutomationCard(onClick: () -> Unit) {
    val showDeviceRestriction = Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
            !EnvironmentUtils.isTelevision() &&
            !EnvironmentUtils.isRooted()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = MiuixIcons.Code,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    tint = MiuixTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_automation_title),
                        style = MiuixTheme.textStyles.title2,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.home_automation_description),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (showDeviceRestriction) {
                Spacer(modifier = Modifier.height(8.dp))
                val context = LocalContext.current
                AndroidView(
                    factory = { ctx ->
                        TextView(ctx).apply {
                            movementMethod = LinkMovementMethod.getInstance()
                            text = ctx.getString(
                                R.string.home_automation_description_device_restriction,
                                "<b><font face=\"monospace\">adb tcpip 5555</font></b>"
                            ).toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
                        }
                    },
                    update = { tv ->
                        tv.text = tv.context.getString(
                            R.string.home_automation_description_device_restriction,
                            "<b><font face=\"monospace\">adb tcpip 5555</font></b>"
                        ).toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
                    },
                )
            }
        }
    }
}

@Composable
private fun MiuixAdbPermissionLimitedCard() {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.home_adb_is_limited_title),
                style = MiuixTheme.textStyles.title2,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_adb_is_limited_description),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                text = stringResource(R.string.home_adb_button_view_help),
                onClick = { CustomTabsHelper.launchUrlOrCopy(context, Helps.ADB.get()) }
            )
        }
    }
}

@Composable
private fun MiuixLearnMoreCard() {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                CustomTabsHelper.launchUrlOrCopy(context, Helps.DEVELOPER.get())
            },
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = MiuixIcons.Help,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                tint = MiuixTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(R.string.home_learn_more_title),
                    style = MiuixTheme.textStyles.title2,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.home_learn_more_description),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
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
            Text(
                text = stringResource(R.string.about_view_source_code, "GitHub"),
                style = MiuixTheme.textStyles.body2,
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
                text = stringResource(R.string.home_adb_dialog_view_command_message, command),
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
                            MaterialAlertDialogBuilder(context)
                                .setTitle(R.string.home_automation_regenerate_token)
                                .setMessage(R.string.home_automation_regenerate_token_message)
                                .setNegativeButton(android.R.string.cancel, null)
                                .setPositiveButton(android.R.string.ok) { _, _ ->
                                    authToken = ShizukuSettings.generateAuthToken()
                                }
                                .show()
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

                OutlinedTextField(
                    value = port,
                    onValueChange = {
                        port = it
                        portError = null
                    },
                    label = stringResource(R.string.dialog_adb_port),
                    isError = portError != null,
                    supportingText = portError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = pairingCode,
                    onValueChange = {
                        pairingCode = it
                        codeError = null
                    },
                    label = stringResource(R.string.dialog_adb_pairing_paring_code),
                    isError = codeError != null,
                    supportingText = codeError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
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
                    ) {
                        if (isPairing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                            )
                        } else {
                            Text(stringResource(android.R.string.ok))
                        }
                    }
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
