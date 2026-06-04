package moe.shizuku.manager.compose.screens

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.AdbStarter
import moe.shizuku.manager.ktx.toHtml
import moe.shizuku.manager.receiver.NotifCancelReceiver
import moe.shizuku.manager.receiver.ShizukuReceiverStarter
import moe.shizuku.manager.settings.BugReportDialogActivity
import moe.shizuku.manager.utils.CustomTabsHelper
import moe.shizuku.manager.utils.EnvironmentUtils
import moe.shizuku.manager.utils.SettingsHelper
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.material.app.LocaleDelegate
import rikka.shizuku.manager.ShizukuLocales
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog
import java.util.Locale

@Composable
fun MiuixSettingsScreen(
    scrollBehavior: top.yukonga.miuix.kmp.basic.ScrollBehavior
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
    var nightMode by remember {
        mutableStateOf(ShizukuSettings.getNightMode())
    }
    var legacyPairing by remember {
        mutableStateOf(ShizukuSettings.getLegacyPairing())
    }
    var themeMode by remember {
        mutableStateOf(ShizukuSettings.getThemeMode())
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
    var batteryOptTarget by remember { mutableStateOf("") }

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
            val message = buildString {
                append(context.getString(R.string.settings_restart_dialog_message))
                if (setting == ShizukuSettings.Keys.KEY_TCP_MODE)
                    append(context.getString(R.string.settings_restart_dialog_message_wifi_required_plain))
            }
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
        WindowDialog(
            show = true,
            onDismissRequest = { showRestartDialog = false },
            title = stringResource(R.string.settings_restart_dialog_title),
        ) {
            Column {
                Text(
                    text = restartDialogMessage,
                    style = MiuixTheme.textStyles.body2,
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    text = stringResource(android.R.string.ok),
                    onClick = {
                        showRestartDialog = false
                        pendingRestartAction?.invoke()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Stop TCP dialog
    if (showStopTcpDialog) {
        WindowDialog(
            show = true,
            onDismissRequest = { showStopTcpDialog = false },
            title = stringResource(android.R.string.dialog_alert_title),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.settings_tcp_mode_dialog_close_port),
                    style = MiuixTheme.textStyles.body2,
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    text = stringResource(android.R.string.ok),
                    onClick = {
                        showStopTcpDialog = false
                        scope.launch {
                            AdbStarter.stopTcp(context, EnvironmentUtils.getAdbTcpPort())
                            if (EnvironmentUtils.getAdbTcpPort() <= 0) {
                                ShizukuSettings.setTcpMode(false)
                                tcpMode = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Start-on-boot bug dialog (Android 11-12)
    if (showStartOnBootBugDialog) {
        WindowDialog(
            show = true,
            onDismissRequest = { showStartOnBootBugDialog = false },
            title = stringResource(android.R.string.dialog_alert_title),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.settings_start_on_boot_bug),
                    style = MiuixTheme.textStyles.body2,
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    text = stringResource(android.R.string.ok),
                    onClick = {
                        showStartOnBootBugDialog = false
                        requestBatteryOptimization("startOnBoot", true)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    val nightModeEntries = listOf(
        stringResource(R.string.follow_system),
        stringResource(R.string.dark_theme_off),
        stringResource(R.string.dark_theme_on),
    )
    val nightModeValues = listOf(
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        AppCompatDelegate.MODE_NIGHT_NO,
        AppCompatDelegate.MODE_NIGHT_YES,
    )
    val nightModeIndex = nightModeValues.indexOf(nightMode).coerceAtLeast(0)

    val localeTags = ShizukuLocales.LOCALES
    val displayLocaleTags = ShizukuLocales.DISPLAY_LOCALES
    val currentLocale = ShizukuSettings.getLocale()
    val currentLocaleTag = if (currentLocale == LocaleDelegate.systemLocale) "SYSTEM"
    else currentLocale.toLanguageTag()
    val currentLocaleIndex = localeTags.indexOf(currentLocaleTag).coerceAtLeast(0)
    val localeEntries = displayLocaleTags.mapIndexed { index, tag ->
        if (index == 0) stringResource(R.string.follow_system)
        else {
            val locale = Locale.forLanguageTag(tag)
            locale.getDisplayName(locale)
        }
    }

    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
            overscrollEffect = null,
        ) {
            // Behavior
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item { SmallTitle(text = stringResource(R.string.settings_behavior)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
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
                                if (
                                    !EnvironmentUtils.isTelevision() &&
                                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                                ) {
                                    showStartOnBootBugDialog = true
                                } else {
                                    requestBatteryOptimization("startOnBoot", true)
                                }
                            } else {
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
                        SwitchPreference(
                            title = stringResource(R.string.settings_tcp_mode),
                            summary = stringResource(R.string.settings_tcp_mode_summary),
                            checked = tcpMode,
                            enabled = EnvironmentUtils.isTlsSupported(),
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
                            ArrowPreference(
                                title = stringResource(R.string.settings_tcp_port),
                                summary = portText,
                                onClick = {
                                    // TODO: Show port edit dialog
                                },
                            )
                        }
                    }
                }
            }

            // Appearance
            item { SmallTitle(text = stringResource(R.string.settings_user_interface)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    val themeEntries = listOf("Material", "Miuix")
                    OverlayDropdownPreference(
                        title = stringResource(R.string.settings_theme_mode),
                        items = themeEntries,
                        selectedIndex = themeMode,
                        onSelectedIndexChange = { index ->
                            if (themeMode != index) {
                                ShizukuSettings.setThemeMode(index)
                                themeMode = index
                            }
                        }
                    )

                    OverlayDropdownPreference(
                        title = stringResource(R.string.dark_theme),
                        items = nightModeEntries,
                        selectedIndex = nightModeIndex,
                        onSelectedIndexChange = { index ->
                            val mode = nightModeValues[index]
                            if (nightMode != mode) {
                                AppCompatDelegate.setDefaultNightMode(mode)
                                nightMode = mode
                            }
                        }
                    )

                    OverlayDropdownPreference(
                        title = stringResource(R.string.settings_language),
                        items = localeEntries,
                        selectedIndex = currentLocaleIndex,
                        onSelectedIndexChange = { index ->
                            val newValue = localeTags[index]
                            val locale: Locale = if ("SYSTEM" == newValue) {
                                LocaleDelegate.systemLocale
                            } else {
                                Locale.forLanguageTag(newValue)
                            }
                            LocaleDelegate.defaultLocale = locale
                            ShizukuSettings.getPreferences().edit()
                                .putString(ShizukuSettings.Keys.KEY_LANGUAGE, newValue)
                                .apply()
                        }
                    )
                }
            }

            // Support
            item { SmallTitle(text = stringResource(R.string.settings_support)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    ArrowPreference(
                        title = stringResource(R.string.settings_help),
                        onClick = {
                            CustomTabsHelper.launchUrlOrCopy(context, "https://github.com/thedjchi/Shizuku/wiki")
                        },
                    )

                    ArrowPreference(
                        title = stringResource(R.string.settings_report_bug),
                        onClick = {
                            context.startActivity(Intent(context, BugReportDialogActivity::class.java))
                        },
                    )

                    ArrowPreference(
                        title = stringResource(R.string.settings_translation),
                        summary = stringResource(R.string.settings_translation_summary, stringResource(R.string.app_name)),
                        onClick = {
                            CustomTabsHelper.launchUrlOrCopy(context, "https://crowdin.com/project/shizuku")
                        },
                    )

                    val contributors = stringResource(R.string.translation_contributors).toHtml().toString()
                    if (contributors.isNotBlank()) {
                        ArrowPreference(
                            title = stringResource(R.string.settings_translation_contributors),
                            summary = contributors,
                            onClick = {},
                        )
                    }
                }
            }

            // Advanced
            if (!EnvironmentUtils.isTelevision()) {
                item { SmallTitle(text = stringResource(R.string.settings_advanced)) }
                item {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    ) {
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
            }

            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}
