package moe.shizuku.manager.compose.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import moe.shizuku.manager.R
import moe.shizuku.manager.home.HomeViewModel
import moe.shizuku.manager.management.AppsViewModel
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Apps
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.VerticalSplit

@Composable
fun MiuixMainScreen(
    homeViewModel: HomeViewModel,
    appsViewModel: AppsViewModel,
    onNavigateToStarter: (isRoot: Boolean, port: Int) -> Unit,
    onNavigateToShellTutorial: () -> Unit,
    onNavigateToAdbPairingTutorial: () -> Unit,
) {
    var selectedIndex by remember { mutableIntStateOf(0) }

    val pages = listOf(
        stringResource(R.string.app_name),
        stringResource(R.string.home_app_management_title),
        stringResource(R.string.settings_title)
    )

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = pages[selectedIndex]
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
                    icon = MiuixIcons.Apps,
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
                )
                1 -> MiuixAppManagementScreen(
                    viewModel = appsViewModel,
                )
                2 -> MiuixSettingsScreen()
            }
        }
    }
}
