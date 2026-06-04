package moe.shizuku.manager.compose.screens

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.text.method.LinkMovementMethod
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.Helps
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.AdbKey
import moe.shizuku.manager.adb.AdbMdns
import moe.shizuku.manager.adb.AdbPairingClient
import moe.shizuku.manager.adb.AdbPairingTutorialActivity
import moe.shizuku.manager.adb.AdbStarter
import moe.shizuku.manager.adb.PreferenceAdbKeyStore
import moe.shizuku.manager.ktx.toHtml
import moe.shizuku.manager.management.ApplicationManagementActivity
import moe.shizuku.manager.model.ServiceStatus
import moe.shizuku.manager.settings.SettingsActivity
import moe.shizuku.manager.starter.StarterActivity
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: moe.shizuku.manager.home.HomeViewModel,
    appsViewModel: moe.shizuku.manager.management.AppsViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToAppManagement: () -> Unit,
    onNavigateToStarter: (isRoot: Boolean, port: Int) -> Unit,
    onNavigateToShellTutorial: () -> Unit,
    onNavigateToAdbPairingTutorial: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        homeViewModel.checkBatteryOptimization()
    }

    LaunchedEffect(shouldShowBatteryOpt) {
        if (shouldShowBatteryOpt) {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.snackbar_battery_optimization_home),
                actionLabel = context.getString(R.string.snackbar_action_fix),
                duration = SnackbarDuration.Indefinite,
            )
            if (result == SnackbarResult.ActionPerformed) {
                SettingsHelper.requestIgnoreBatteryOptimizations(context, null)
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
    if (showStopDialog) {
        StopShizukuDialog(
            onDismiss = { showStopDialog = false },
            onConfirm = {
                ShizukuStateMachine.set(ShizukuStateMachine.State.STOPPING)
                runCatching { Shizuku.exit() }
            },
        )
    }
    if (showAutomationSheet) {
        AutomationBottomSheet(onDismiss = { showAutomationSheet = false })
    }
    if (showAdbCommandDialog) {
        AdbCommandDialog(onDismiss = { showAdbCommandDialog = false })
    }
    if (showAdbDiscoveryDialog) {
        AdbDiscoveryDialog(
            onDismiss = { showAdbDiscoveryDialog = false },
            onPortDiscovered = { port ->
                showAdbDiscoveryDialog = false
                onNavigateToStarter(false, port)
            },
        )
    }
    if (showAdbPairDialog) {
        AdbPairDialog(
            onDismiss = { showAdbPairDialog = false },
            onPairSuccess = {
                showAdbPairDialog = false
                Toast.makeText(context, context.getString(R.string.notification_adb_pairing_succeed_title), Toast.LENGTH_SHORT).show()
            },
        )
    }
    if (showWadbNotEnabledDialog) {
        WadbNotEnabledDialog(onDismiss = { showWadbNotEnabledDialog = false })
    }
    if (showUsbDebuggingNotEnabledDialog) {
        UsbDebuggingNotEnabledDialog(onDismiss = { showUsbDebuggingNotEnabledDialog = false })
    }
    if (showAccessibilityDialog) {
        AccessibilityDialog(
            onDismiss = { showAccessibilityDialog = false },
            onNavigateToSettings = { page ->
                showAccessibilityDialog = false
                page.launch(context)
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings_title)) },
                            leadingIcon = { Icon(Icons.Default.Settings, null) },
                            onClick = {
                                menuExpanded = false
                                onNavigateToSettings()
                            },
                        )
                        if (isRunning) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_stop)) },
                                leadingIcon = { Icon(Icons.Filled.Stop, null) },
                                onClick = {
                                    menuExpanded = false
                                    showStopDialog = true
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_about)) },
                            leadingIcon = { Icon(Icons.Default.Info, null) },
                            onClick = {
                                menuExpanded = false
                                showAboutDialog = true
                            },
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                ServerStatusCard(status = status)
            }

            if (adbPermission) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    ManageAppsCard(
                        grantedCount = grantedCount,
                        onClick = onNavigateToAppManagement,
                    )
                }
                item(span = StaggeredGridItemSpan.FullLine) {
                    TerminalCard(onClick = onNavigateToShellTutorial)
                }
            }

            if (isRunning && !adbPermission) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    AdbPermissionLimitedCard()
                }
            }

            if (isPrimaryUser) {
                val rootRestart = isRunning && isRoot
                if (EnvironmentUtils.isRooted()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        StartRootCard(
                            isRestart = rootRestart,
                            onClick = { onNavigateToStarter(true, 0) },
                        )
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ||
                    EnvironmentUtils.isTelevision() ||
                    EnvironmentUtils.getAdbTcpPort() > 0
                ) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        StartWirelessAdbCard(
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

                item(span = StaggeredGridItemSpan.FullLine) {
                    StartAdbCard(onClick = { showAdbCommandDialog = true })
                }
            }

            item(span = StaggeredGridItemSpan.FullLine) {
                AutomationCard(onClick = { showAutomationSheet = true })
            }

            item(span = StaggeredGridItemSpan.FullLine) {
                LearnMoreCard()
            }
        }
    }
}

// --- Helper object for wireless ADB start ---
internal object StartWirelessAdbHelper {
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
            // mDNS discovery - launch pairing
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
private fun ServerStatusCard(status: ServiceStatus) {
    val isRunning = status.isRunning
    val isRoot = status.uid == 0
    val user = if (isRoot) "root" else "adb"
    val icon = if (isRunning) Icons.Filled.Security else Icons.Filled.Warning

    val latestApiVersion = Shizuku.getLatestServiceVersion()
    val latestPatchVersion = ShizukuApiConstants.SERVER_PATCH_VERSION
    val hasUpdate = isRunning && (status.apiVersion != latestApiVersion || status.patchVersion != latestPatchVersion)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                tint = if (isRunning) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onErrorContainer,
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
                    style = MaterialTheme.typography.titleMedium,
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
                    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
                    AndroidView(
                        factory = { ctx ->
                            TextView(ctx).apply {
                                text = versionText.toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
                                textSize = 14f
                                setTextColor(onSurfaceVariantColor.toArgb())
                            }
                        },
                        update = { tv ->
                            tv.text = versionText.toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
                            tv.setTextColor(onSurfaceVariantColor.toArgb())
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ManageAppsCard(grantedCount: Int, onClick: () -> Unit) {
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
                imageVector = Icons.Default.Apps,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_app_management_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = pluralStringResource(R.plurals.home_app_management_authorized_apps_count, grantedCount, grantedCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TerminalCard(onClick: () -> Unit) {
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
                imageVector = Icons.Default.Terminal,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(R.string.home_terminal_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.home_terminal_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StartRootCard(isRestart: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.home_root_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
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
private fun StartWirelessAdbCard(
    onStartWadb: () -> Unit,
    onPair: () -> Unit,
    onViewGuide: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Icon(
                imageVector = Icons.Default.Devices,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.home_wireless_adb_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            AndroidView(
                factory = { ctx ->
                    TextView(ctx).apply {
                        text = ctx.getString(R.string.home_wireless_adb_description)
                            .toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
                    }
                },
                update = { tv ->
                    tv.text = tv.context.getString(R.string.home_wireless_adb_description)
                        .toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (EnvironmentUtils.isTlsSupported()) {
                OutlinedButton(onClick = onViewGuide) {
                    Text(stringResource(R.string.home_wireless_adb_view_guide_button))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStartWadb) {
                    Text(stringResource(R.string.start))
                }
                if (EnvironmentUtils.isTlsSupported()) {
                    OutlinedButton(onClick = onPair) {
                        Text(stringResource(R.string.adb_pairing))
                    }
                }
            }
        }
    }
}

@Composable
private fun StartAdbCard(onClick: () -> Unit) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Adb,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.home_adb_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.home_adb_button_view_command),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
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
private fun AutomationCard(onClick: () -> Unit) {
    val showDeviceRestriction = Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
            !EnvironmentUtils.isTelevision() &&
            !EnvironmentUtils.isRooted()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_automation_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.home_automation_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (showDeviceRestriction) {
                Spacer(modifier = Modifier.height(8.dp))
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
private fun AdbPermissionLimitedCard() {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.home_adb_is_limited_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_adb_is_limited_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = {
                CustomTabsHelper.launchUrlOrCopy(context, Helps.ADB.get())
            }) {
                Text(stringResource(R.string.home_adb_button_view_help))
            }
        }
    }
}

@Composable
private fun LearnMoreCard() {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                CustomTabsHelper.launchUrlOrCopy(context, Helps.DEVELOPER.get())
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(R.string.home_learn_more_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.home_learn_more_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// --- Dialogs ---

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_name)) },
        text = {
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.about_dialog_button_close))
            }
        },
    )
}

@Composable
private fun StopShizukuDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(stringResource(R.string.dialog_stop_message)) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun AdbCommandDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val command = "adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_adb_button_view_command)) },
        text = {
            Column {
                AndroidView(
                    factory = { ctx ->
                        TextView(ctx).apply {
                            text = ctx.getString(R.string.home_adb_dialog_view_command_message, command)
                                .toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
                        }
                    },
                    update = { tv ->
                        tv.text = tv.context.getString(R.string.home_adb_dialog_view_command_message, command)
                            .toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("shizuku_command", command))
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                    Toast.makeText(context, context.getString(R.string.toast_copied_to_clipboard), Toast.LENGTH_SHORT).show()
                }
                onDismiss()
            }) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.home_adb_dialog_view_command_copy_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun AutomationBottomSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var authToken by remember { mutableStateOf(ShizukuSettings.getAuthToken()) }
    var selectedAction by remember { mutableIntStateOf(0) } // 0 = START, 1 = STOP
    val startAction = "${BuildConfig.APPLICATION_ID}.START"
    val stopAction = "${BuildConfig.APPLICATION_ID}.STOP"
    val currentAction = if (selectedAction == 0) startAction else stopAction

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_automation_bottom_sheet_intents)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Action radio buttons
                Column {
                    Text(
                        text = stringResource(R.string.home_automation_bottom_sheet_label_action),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        style = MaterialTheme.typography.bodyMedium,
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
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .clickable { selectedAction = 1 }
                            .weight(1f),
                    )
                }

                // Action field
                AutomationIntentRow(
                    label = stringResource(R.string.home_automation_bottom_sheet_label_action),
                    value = currentAction,
                )

                // Package field
                AutomationIntentRow(
                    label = stringResource(R.string.home_automation_bottom_sheet_label_package),
                    value = context.packageName,
                )

                // Target field
                AutomationIntentRow(
                    label = stringResource(R.string.home_automation_bottom_sheet_label_target),
                    value = "Broadcast Receiver",
                )

                // Extras/Auth token field with regenerate
                Column {
                    Text(
                        text = stringResource(R.string.home_automation_bottom_sheet_label_extras),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "auth_token=$authToken",
                            style = MaterialTheme.typography.bodyMedium,
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
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.home_automation_regenerate_token),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        },
    )
}

@Composable
private fun AutomationIntentRow(label: String, value: String) {
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
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// --- ADB Discovery Dialog (mDNS) ---

@Composable
private fun AdbDiscoveryDialog(
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
        // Enable wireless debugging if we have permission
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

    AlertDialog(
        onDismissRequest = {
            adbMdns.stop()
            onDismiss()
        },
        title = { Text(stringResource(R.string.dialog_adb_discovery)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.dialog_adb_discovery_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.dialog_adb_discovery_message_toggle_wireless_debugging),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                adbMdns.stop()
                onDismiss()
                SettingsPage.Developer.HighlightWirelessDebugging.launch(context)
            }) {
                Text(stringResource(R.string.development_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                adbMdns.stop()
                onDismiss()
            }) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

// --- ADB Pair Dialog ---

@Composable
private fun AdbPairDialog(
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

    AlertDialog(
        onDismissRequest = {
            adbMdns.stop()
            onDismiss()
        },
        title = {
            Text(
                stringResource(
                    if (isDiscovering && inMultiScreenOrDisplay) R.string.dialog_adb_pairing_discovery
                    else R.string.dialog_adb_pairing_title
                )
            )
        },
        text = {
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
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    if (inMultiScreenOrDisplay) {
                        Text(
                            text = stringResource(R.string.dialog_adb_pairing_message),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.dialog_adb_pairing_message),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    OutlinedTextField(
                        value = port,
                        onValueChange = {
                            port = it
                            portError = null
                        },
                        label = { Text(stringResource(R.string.dialog_adb_port)) },
                        isError = portError != null,
                        supportingText = portError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = pairingCode,
                        onValueChange = {
                            pairingCode = it
                            codeError = null
                        },
                        label = { Text(stringResource(R.string.dialog_adb_pairing_paring_code)) },
                        isError = codeError != null,
                        supportingText = codeError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            if (!isDiscovering || !inMultiScreenOrDisplay) {
                TextButton(
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
                ) {
                    if (isPairing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                adbMdns.stop()
                onDismiss()
            }) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

// --- Wadb Not Enabled Dialog ---

@Composable
private fun WadbNotEnabledDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(stringResource(R.string.dialog_wireless_adb_not_enabled)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        },
    )
}

// --- USB Debugging Not Enabled Dialog ---

@Composable
private fun UsbDebuggingNotEnabledDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(stringResource(R.string.dialog_usb_debugging_not_enabled)) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                SettingsPage.Developer.HighlightUsbDebugging.launch(context)
            }) {
                Text(stringResource(R.string.development_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

// --- Accessibility Dialog (for TV devices) ---

@Composable
private fun AccessibilityDialog(
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
            // Show navigate dialog
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.dialog_adb_pairing_title)) },
                text = { Text(stringResource(R.string.dialog_adb_pairing_accessibility_navigate)) },
                confirmButton = {
                    TextButton(onClick = {
                        onDismiss()
                        SettingsPage.Developer.HighlightWirelessDebugging.launch(context)
                    }) {
                        Text(stringResource(R.string.development_settings))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
        }
        hasWriteSecureSettings -> {
            // Try to enable accessibility service directly
            val enabled = remember { enableAccessibilityService(context) }
            if (enabled) {
                // Already enabled, show navigate dialog
                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { Text(stringResource(R.string.dialog_adb_pairing_title)) },
                    text = { Text(stringResource(R.string.dialog_adb_pairing_accessibility_navigate)) },
                    confirmButton = {
                        TextButton(onClick = {
                            onDismiss()
                            SettingsPage.Developer.HighlightWirelessDebugging.launch(context)
                        }) {
                            Text(stringResource(R.string.development_settings))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(android.R.string.cancel))
                        }
                    },
                )
            } else {
                // Show permission dialog
                ShowPermissionDialog(onDismiss = onDismiss, onContinue = {
                    onNavigateToSettings(SettingsPage.Accessibility)
                })
            }
        }
        !hasAccessRestrictedSettings -> {
            // Show permission dialog
            ShowPermissionDialog(onDismiss = onDismiss, onContinue = {
                onNavigateToSettings(SettingsPage.Accessibility)
            })
        }
        else -> {
            // Show enable dialog
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.dialog_adb_pairing_title)) },
                text = { Text(stringResource(R.string.dialog_adb_pairing_accessibility_enable)) },
                confirmButton = {
                    TextButton(onClick = {
                        onDismiss()
                        SettingsPage.Accessibility.launch(context)
                    }) {
                        Text(stringResource(R.string.enable))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun ShowPermissionDialog(
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
) {
    val context = LocalContext.current
    val permissionName = "ACCESS_RESTRICTED_SETTINGS"
    val permissionCommand = "adb shell cmd appops set ${context.packageName} $permissionName allow"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(android.R.string.dialog_alert_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.dialog_adb_pairing_accessibility_permission, permissionName, permissionCommand),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onContinue()
            }) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
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
