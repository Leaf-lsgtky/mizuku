package moe.shizuku.manager.compose.screens

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import moe.shizuku.manager.R
import moe.shizuku.manager.adb.AdbPairingService
import moe.shizuku.manager.compose.components.miuix.Os4Scaffold
import moe.shizuku.manager.compose.theme.LocalIsMiuix
import moe.shizuku.manager.utils.SettingsHelper
import moe.shizuku.manager.utils.SettingsPage
import rikka.compatibility.DeviceCompatibility
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbPairingTutorialScreen(
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    var notificationEnabled by remember {
        val nm = context.getSystemService(NotificationManager::class.java)
        val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.getNotificationChannel(AdbPairingService.NOTIFICATION_CHANNEL)
        } else null
        mutableStateOf(
            nm.areNotificationsEnabled() &&
                    (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE)
        )
    }

    if (LocalIsMiuix.current) {
        Os4Scaffold(
            title = stringResource(R.string.adb_pairing_tutorial_title),
            onBack = onNavigateBack,
        ) { paddingValues, scrollBehavior, contentModifier ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(contentModifier)
                    .overScrollVertical(onOffsetChanged = { scrollBehavior.state.overscrollOffset = it })
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = paddingValues.calculateTopPadding() + 12.dp,
                        bottom = paddingValues.calculateBottomPadding() + 16.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MiuixPairingTutorialContent(notificationEnabled)
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.adb_pairing_tutorial_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (notificationEnabled) {
                // Notification info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.adb_pairing_tutorial_content_notification),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }

                // Network info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.adb_pairing_tutorial_content_network),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.adb_pairing_tutorial_content_network_limation_not_foreground),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        )
                    }
                }

                // MIUI warning card
                if (DeviceCompatibility.isMiui()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.adb_pairing_tutorial_content_miui),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.adb_pairing_tutorial_content_miui_2),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }

                // Step 1
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.adb_pairing_tutorial_content_steps),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            SettingsHelper.launchOrHighlightWirelessDebugging(context)
                        }) {
                            Text(stringResource(R.string.development_settings))
                        }
                    }
                }

                // Step 2
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.adb_pairing_tutorial_content_enter_pairing_code),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }

                // Step 3
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.adb_pairing_tutorial_content_finish),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            } else {
                // Notification disabled warning card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.adb_pairing_tutorial_content_notification_blocked),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                        ) {
                            Text(stringResource(R.string.notification_settings))
                        }
                    }
                }
            }

            // Notification settings button (always visible)
            if (notificationEnabled) {
                OutlinedButton(onClick = {
                    SettingsPage.Notifications.NotificationSettings.launch(context)
                }) {
                    Text(stringResource(R.string.notification_settings))
                }
            }
        }
    }
}

@Composable
private fun MiuixPairingTutorialContent(notificationEnabled: Boolean) {
    val context = LocalContext.current

    if (notificationEnabled) {
        MiuixPairingInstructionCard {
            MiuixText(
                text = stringResource(R.string.adb_pairing_tutorial_content_notification),
                style = MiuixTheme.textStyles.body1,
            )
        }
        MiuixPairingInstructionCard {
            MiuixText(
                text = stringResource(R.string.adb_pairing_tutorial_content_network),
                style = MiuixTheme.textStyles.body1,
            )
            Spacer(Modifier.height(8.dp))
            MiuixText(
                text = stringResource(R.string.adb_pairing_tutorial_content_network_limation_not_foreground),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        if (DeviceCompatibility.isMiui()) {
            MiuixPairingInstructionCard {
                MiuixText(
                    text = stringResource(R.string.adb_pairing_tutorial_content_miui),
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.error,
                )
                Spacer(Modifier.height(8.dp))
                MiuixText(
                    text = stringResource(R.string.adb_pairing_tutorial_content_miui_2),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.error,
                )
            }
        }
        MiuixPairingInstructionCard {
            MiuixText(
                text = stringResource(R.string.adb_pairing_tutorial_content_steps),
                style = MiuixTheme.textStyles.body1,
            )
            Spacer(Modifier.height(12.dp))
            MiuixTextButton(
                text = stringResource(R.string.development_settings),
                onClick = { SettingsHelper.launchOrHighlightWirelessDebugging(context) },
                colors = MiuixButtonDefaults.textButtonColorsPrimary(),
            )
        }
        MiuixPairingInstructionCard {
            MiuixText(
                text = stringResource(R.string.adb_pairing_tutorial_content_enter_pairing_code),
                style = MiuixTheme.textStyles.body1,
            )
        }
        MiuixPairingInstructionCard {
            MiuixText(
                text = stringResource(R.string.adb_pairing_tutorial_content_finish),
                style = MiuixTheme.textStyles.body1,
            )
        }
        MiuixTextButton(
            text = stringResource(R.string.notification_settings),
            onClick = { SettingsPage.Notifications.NotificationSettings.launch(context) },
        )
    } else {
        MiuixPairingInstructionCard {
            MiuixText(
                text = stringResource(R.string.adb_pairing_tutorial_content_notification_blocked),
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.error,
            )
            Spacer(Modifier.height(12.dp))
            MiuixTextButton(
                text = stringResource(R.string.notification_settings),
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                    )
                },
                colors = MiuixButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

@Composable
private fun MiuixPairingInstructionCard(content: @Composable () -> Unit) {
    MiuixCard(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(16.dp),
    ) {
        content()
    }
}
