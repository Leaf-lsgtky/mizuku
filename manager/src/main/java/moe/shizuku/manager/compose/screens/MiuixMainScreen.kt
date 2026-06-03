package moe.shizuku.manager.compose.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import moe.shizuku.manager.R
import moe.shizuku.manager.home.HomeViewModel
import moe.shizuku.manager.management.AppsViewModel
import moe.shizuku.manager.utils.ShizukuStateMachine
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.All
import top.yukonga.miuix.kmp.icon.extended.MoreCircle
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.VerticalSplit
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun MiuixMainScreen(
    homeViewModel: HomeViewModel,
    appsViewModel: AppsViewModel,
    onNavigateToStarter: (isRoot: Boolean, port: Int) -> Unit,
    onNavigateToShellTutorial: () -> Unit,
    onNavigateToAdbPairingTutorial: () -> Unit,
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val scrollBehavior = MiuixScrollBehavior()

    var showStopDialog by remember { mutableStateOf(false) }
    val showTopPopup = remember { mutableStateOf(false) }

    if (showStopDialog) {
        WindowDialog(
            show = true,
            onDismissRequest = { showStopDialog = false },
        ) {
            Column {
                Text(
                    text = stringResource(R.string.dialog_stop_message),
                    style = MiuixTheme.textStyles.body2,
                )
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = { showStopDialog = false },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        text = stringResource(android.R.string.ok),
                        onClick = {
                            ShizukuStateMachine.set(ShizukuStateMachine.State.STOPPING)
                            runCatching { rikka.shizuku.Shizuku.exit() }
                            showStopDialog = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    val pages = listOf(
        stringResource(R.string.app_name),
        stringResource(R.string.home_app_management_title),
        stringResource(R.string.settings_title)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = pages[selectedIndex],
                scrollBehavior = scrollBehavior,
                actions = {
                    if (selectedIndex == 0) {
                        Box {
                            IconButton(
                                onClick = { showTopPopup.value = true },
                                holdDownState = showTopPopup.value
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.MoreCircle,
                                    tint = MiuixTheme.colorScheme.onSurface,
                                    contentDescription = null
                                )
                            }
                            OverlayListPopup(
                                show = showTopPopup.value,
                                onDismissRequest = { showTopPopup.value = false },
                                content = {
                                    ListPopupColumn {
                                        DropdownImpl(
                                            text = stringResource(R.string.action_stop),
                                            optionSize = 1,
                                            isSelected = false,
                                            onSelectedIndexChange = {
                                                showTopPopup.value = false
                                                showStopDialog = true
                                            },
                                            index = 0
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedIndex == 0,
                    onClick = { selectedIndex = 0 },
                    icon = MiuixIcons.VerticalSplit,
                    label = stringResource(R.string.app_name)
                )
                NavigationBarItem(
                    selected = selectedIndex == 1,
                    onClick = { selectedIndex = 1 },
                    icon = MiuixIcons.All,
                    label = stringResource(R.string.home_app_management_title)
                )
                NavigationBarItem(
                    selected = selectedIndex == 2,
                    onClick = { selectedIndex = 2 },
                    icon = MiuixIcons.Settings,
                    label = stringResource(R.string.settings_title)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedIndex) {
                0 -> MiuixHomeScreen(
                    homeViewModel = homeViewModel,
                    appsViewModel = appsViewModel,
                    onNavigateToStarter = onNavigateToStarter,
                    onNavigateToShellTutorial = onNavigateToShellTutorial,
                    onNavigateToAdbPairingTutorial = onNavigateToAdbPairingTutorial,
                    scrollBehavior = scrollBehavior
                )
                1 -> MiuixAppManagementScreen(
                    viewModel = appsViewModel,
                    scrollBehavior = scrollBehavior
                )
                2 -> MiuixSettingsScreen(
                    scrollBehavior = scrollBehavior
                )
            }
        }
    }
}
