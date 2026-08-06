package moe.shizuku.manager.compose.screens

import android.content.Intent
import android.os.Build
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.AdbStarter
import moe.shizuku.manager.compose.components.EditTextPreference
import moe.shizuku.manager.compose.components.Preference
import moe.shizuku.manager.compose.components.PreferenceCategory
import moe.shizuku.manager.compose.components.SwitchPreference
import moe.shizuku.manager.ktx.toHtml
import rikka.html.text.HtmlCompat
import moe.shizuku.manager.receiver.NotifCancelReceiver
import moe.shizuku.manager.receiver.ShizukuReceiverStarter
import moe.shizuku.manager.settings.BugReportDialogActivity
import moe.shizuku.manager.utils.CustomTabsHelper
import moe.shizuku.manager.utils.EnvironmentUtils
import moe.shizuku.manager.utils.SettingsHelper
import moe.shizuku.manager.utils.ShizukuStateMachine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAppearance: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var startOnBoot by remember {
        mutableStateOf(ShizukuSettings.getStartOnBoot(context))
    }
    var watchdog by remember {
        mutableStateOf(ShizukuSettings.isWatchdogRunning())
    }
    var tcpMode by remember {
        mutableStateOf(ShizukuSettings.getTcpMode())
    }
    var tcpPort by remember {
        mutableStateOf(ShizukuSettings.getTcpPort())
    }
    var legacyPairing by remember {
        mutableStateOf(ShizukuSettings.getLegacyPairing())
    }

    // Dialog states
    var showRestartDialog by remember { mutableStateOf(false) }
    var restartDialogMessage by remember { mutableStateOf("") }
    var pendingRestartAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showStopTcpDialog by remember { mutableStateOf(false) }
    var showStartOnBootBugDialog by remember { mutableStateOf(false) }

    // Battery optimization state machine
    var batteryOptPending by remember { mutableStateOf(false) }
    var batteryOptNewValue by remember { mutableStateOf(false) }
    var batteryOptTarget by remember { mutableStateOf("") } // "startOnBoot" or "watchdog"

    val batteryOptLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val accepted = SettingsHelper.isIgnoringBatteryOptimizations(context)
        if (batteryOptPending) {
            batteryOptPending = false
            if (accepted) {
                when (batteryOptTarget) {
                    "startOnBoot" -> {
                        ShizukuSettings.setStartOnBoot(context, batteryOptNewValue)
                        startOnBoot = batteryOptNewValue
                    }
                    "watchdog" -> {
                        ShizukuSettings.setWatchdog(context, batteryOptNewValue)
                        watchdog = batteryOptNewValue
                    }
                }
            }
            // If not accepted, setting is not applied (reverts)
        }
    }

    fun needsRestart(setting: String, newValue: Any? = null): Boolean {
        val currentPort = EnvironmentUtils.getAdbTcpPort()
        return when (setting) {
            ShizukuSettings.Keys.KEY_TCP_MODE -> {
                val newMode = newValue as? Boolean ?: ShizukuSettings.getTcpMode()
                (currentPort > 0) != newMode
            }
            ShizukuSettings.Keys.KEY_TCP_PORT -> {
                val newPort = newValue as? Int ?: ShizukuSettings.getTcpPort()
                (currentPort > 0) && (currentPort != newPort)
            }
            else -> false
        }
    }

    fun applyWithRestartPrompt(setting: String, newValue: Any? = null, applyChange: () -> Unit) {
        if (!ShizukuStateMachine.isRunning() || !needsRestart(setting, newValue)) {
            applyChange()
            context.sendBroadcast(Intent(context, NotifCancelReceiver::class.java))
        } else {
            val message = context.getString(R.string.settings_restart_dialog_message) +
                if (setting == ShizukuSettings.Keys.KEY_TCP_MODE)
                    context.getString(R.string.settings_restart_dialog_message_wifi_required)
                else ""
            restartDialogMessage = message
            pendingRestartAction = {
                applyChange()
                ShizukuReceiverStarter.start(context, true)
            }
            showRestartDialog = true
        }
    }

    fun requestBatteryOptimization(target: String, newValue: Boolean) {
        if (SettingsHelper.isIgnoringBatteryOptimizations(context) || EnvironmentUtils.isTelevision()) {
            when (target) {
                "startOnBoot" -> {
                    ShizukuSettings.setStartOnBoot(context, newValue)
                    startOnBoot = newValue
                }
                "watchdog" -> {
                    ShizukuSettings.setWatchdog(context, newValue)
                    watchdog = newValue
                }
            }
            return
        }
        batteryOptTarget = target
        batteryOptNewValue = newValue
        batteryOptPending = true
        SettingsHelper.requestIgnoreBatteryOptimizations(context, batteryOptLauncher)
    }

    // Restart dialog
    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text(stringResource(R.string.settings_restart_dialog_title)) },
            text = {
                AndroidView(
                    factory = { ctx ->
                        TextView(ctx).apply {
                            text = restartDialogMessage.toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
                        }
                    },
                    update = { tv ->
                        tv.text = restartDialogMessage.toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRestartDialog = false
                    pendingRestartAction?.invoke()
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    // Stop TCP dialog
    if (showStopTcpDialog) {
        AlertDialog(
            onDismissRequest = { showStopTcpDialog = false },
            title = { Text(stringResource(android.R.string.dialog_alert_title)) },
            text = {
                Text(
                    text = stringResource(R.string.settings_tcp_mode_dialog_close_port),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showStopTcpDialog = false
                    scope.launch {
                        AdbStarter.stopTcp(context, EnvironmentUtils.getAdbTcpPort())
                        if (EnvironmentUtils.getAdbTcpPort() <= 0) {
                            ShizukuSettings.setTcpMode(false)
                            tcpMode = false
                        }
                    }
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopTcpDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    // Start-on-boot bug dialog (Android 11-12)
    if (showStartOnBootBugDialog) {
        AlertDialog(
            onDismissRequest = { showStartOnBootBugDialog = false },
            title = { Text(stringResource(android.R.string.dialog_alert_title)) },
            text = {
                Text(
                    text = stringResource(R.string.settings_start_on_boot_bug),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showStartOnBootBugDialog = false
                    requestBatteryOptimization("startOnBoot", true)
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartOnBootBugDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            // Behavior
            PreferenceCategory(title = stringResource(R.string.settings_behavior)) {
                val canStartOnBoot = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ||
                        EnvironmentUtils.isTelevision() ||
                        EnvironmentUtils.isRooted()

                SwitchPreference(
                    title = stringResource(R.string.settings_start_on_boot),
                    summary = if (canStartOnBoot) null else stringResource(R.string.settings_start_on_boot_summary),
                    checked = startOnBoot,
                    enabled = canStartOnBoot,
                    onCheckedChange = { newValue ->
                        if (newValue) {
                            // Turning ON
                            if (
                                !EnvironmentUtils.isTelevision() &&
                                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                            ) {
                                // Android 11-12 has a bug with wireless debugging not remembering networks
                                showStartOnBootBugDialog = true
                            } else {
                                requestBatteryOptimization("startOnBoot", true)
                            }
                        } else {
                            // Turning OFF - always allow
                            ShizukuSettings.setStartOnBoot(context, false)
                            startOnBoot = false
                        }
                    },
                )

                SwitchPreference(
                    title = stringResource(R.string.settings_watchdog),
                    summary = stringResource(R.string.settings_watchdog_summary),
                    checked = watchdog,
                    onCheckedChange = { newValue ->
                        if (newValue) {
                            requestBatteryOptimization("watchdog", true)
                        } else {
                            ShizukuSettings.setWatchdog(context, false)
                            watchdog = false
                        }
                    },
                )

                if (EnvironmentUtils.isTlsSupported() || EnvironmentUtils.isTelevision()) {
                    val tcpNeedsRestart = needsRestart(ShizukuSettings.Keys.KEY_TCP_MODE)
                    SwitchPreference(
                        title = stringResource(R.string.settings_tcp_mode),
                        summary = stringResource(R.string.settings_tcp_mode_summary),
                        checked = tcpMode,
                        enabled = EnvironmentUtils.isTlsSupported(),
                        icon = if (tcpNeedsRestart) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.settings_restart_dialog_title),
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else null,
                        onCheckedChange = { newValue ->
                            if (!newValue && !ShizukuStateMachine.isRunning() && needsRestart(ShizukuSettings.Keys.KEY_TCP_MODE, newValue)) {
                                showStopTcpDialog = true
                            } else {
                                applyWithRestartPrompt(ShizukuSettings.Keys.KEY_TCP_MODE, newValue) {
                                    ShizukuSettings.setTcpMode(newValue)
                                    tcpMode = newValue
                                }
                            }
                        },
                    )

                    if (tcpMode) {
                        val portText = if (tcpPort == 5555) stringResource(R.string.settings_tcp_port_default)
                        else tcpPort.toString()
                        val portNeedsRestart = needsRestart(ShizukuSettings.Keys.KEY_TCP_PORT)
                        EditTextPreference(
                            title = stringResource(R.string.settings_tcp_port),
                            summary = portText,
                            text = if (tcpPort == 5555) null else tcpPort.toString(),
                            hint = stringResource(R.string.settings_tcp_port_hint),
                            icon = if (portNeedsRestart) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.settings_restart_dialog_title),
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            } else null,
                            onValueChange = { newText ->
                                val port = newText?.toIntOrNull()
                                if (port == null || port in 1..65535) {
                                    applyWithRestartPrompt(ShizukuSettings.Keys.KEY_TCP_PORT, port ?: 5555) {
                                        ShizukuSettings.setTcpPort(port)
                                        tcpPort = port ?: 5555
                                    }
                                }
                            },
                        )
                    }
                }
            }

            // Appearance — every option lives on its own screen now.
            PreferenceCategory(title = stringResource(R.string.settings_user_interface)) {
                Preference(
                    title = stringResource(R.string.settings_user_interface),
                    summary = stringResource(R.string.settings_user_interface_summary),
                    onClick = onNavigateToAppearance,
                )
            }

            // Support
            PreferenceCategory(title = stringResource(R.string.settings_support)) {
                Preference(
                    title = stringResource(R.string.settings_help),
                    onClick = {
                        CustomTabsHelper.launchUrlOrCopy(context, "https://github.com/thedjchi/Shizuku/wiki")
                    },
                )
                Preference(
                    title = stringResource(R.string.settings_report_bug),
                    onClick = {
                        context.startActivity(Intent(context, BugReportDialogActivity::class.java))
                    },
                )
                Preference(
                    title = stringResource(R.string.settings_translation),
                    summary = stringResource(R.string.settings_translation_summary, stringResource(R.string.app_name)),
                    onClick = {
                        CustomTabsHelper.launchUrlOrCopy(context, "https://crowdin.com/project/shizuku")
                    },
                )
                val contributors = stringResource(R.string.translation_contributors).toHtml().toString()
                if (contributors.isNotBlank()) {
                    Preference(
                        title = stringResource(R.string.settings_translation_contributors),
                        summary = contributors,
                        onClick = {},
                    )
                }
            }

            // Advanced
            if (!EnvironmentUtils.isTelevision()) {
                PreferenceCategory(title = stringResource(R.string.settings_advanced)) {
                    SwitchPreference(
                        title = stringResource(R.string.settings_legacy_pairing),
                        summary = stringResource(R.string.settings_legacy_pairing_summary),
                        checked = legacyPairing,
                        onCheckedChange = { newValue ->
                            ShizukuSettings.getPreferences().edit()
                                .putBoolean(ShizukuSettings.Keys.KEY_LEGACY_PAIRING, newValue)
                                .apply()
                            legacyPairing = newValue
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
