package moe.shizuku.manager.compose.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import moe.shizuku.manager.R
import moe.shizuku.manager.compose.components.SearchBarFake
import moe.shizuku.manager.compose.components.SearchPager
import moe.shizuku.manager.compose.components.SearchStatus
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
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.icon.extended.VerticalSplit
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
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
    val density = LocalDensity.current

    var showStopDialog by remember { mutableStateOf(false) }
    val showTopPopup = remember { mutableStateOf(false) }
    
    // 应用管理页面的排序和筛选状态
    var sortOption by remember { mutableIntStateOf(0) }
    var showSystemApps by remember { mutableStateOf(false) }
    
    // 搜索状态
    val searchLabel = stringResource(R.string.search_apps)
    var searchStatus by remember { mutableStateOf(SearchStatus(searchLabel)) }
    var searchResults by remember { mutableStateOf<List<android.content.pm.PackageInfo>>(emptyList()) }
    val dynamicTopPadding by remember {
        derivedStateOf { 12.dp * (1f - scrollBehavior.state.collapsedFraction) }
    }

    // 返回按钮处理：取消搜索
    BackHandler(enabled = !searchStatus.isCollapsed()) {
        searchStatus = searchStatus.copy(
            searchText = "",
            current = SearchStatus.Status.COLLAPSING
        )
    }

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
            searchStatus.TopAppBarAnim {
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
                        } else if (selectedIndex == 1 && searchStatus.isCollapsed()) {
                            // 应用管理页面的排序按钮
                            Box {
                                val showSortPopup = remember { mutableStateOf(false) }
                                OverlayListPopup(
                                    show = showSortPopup.value,
                                    onDismissRequest = { showSortPopup.value = false },
                                    content = {
                                        ListPopupColumn {
                                            val sortOptions = listOf(
                                                R.string.sort_by_name,
                                                R.string.sort_by_package_name,
                                            )
                                            sortOptions.forEachIndexed { index, resId ->
                                                DropdownImpl(
                                                    text = stringResource(resId),
                                                    optionSize = sortOptions.size,
                                                    isSelected = sortOption == index,
                                                    index = index,
                                                    onSelectedIndexChange = {
                                                        sortOption = index
                                                        showSortPopup.value = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                )
                                IconButton(
                                    onClick = { showSortPopup.value = true },
                                    holdDownState = showSortPopup.value,
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.Sort,
                                        tint = MiuixTheme.colorScheme.onSurface,
                                        contentDescription = stringResource(R.string.menu_sort)
                                    )
                                }
                            }

                            // 应用管理页面的更多选项按钮
                            Box {
                                val showMorePopup = remember { mutableStateOf(false) }
                                OverlayListPopup(
                                    show = showMorePopup.value,
                                    onDismissRequest = { showMorePopup.value = false },
                                    content = {
                                        ListPopupColumn {
                                            DropdownImpl(
                                                text = stringResource(R.string.show_system_apps),
                                                optionSize = 1,
                                                isSelected = showSystemApps,
                                                index = 0,
                                                onSelectedIndexChange = {
                                                    showSystemApps = !showSystemApps
                                                    showMorePopup.value = false
                                                }
                                            )
                                        }
                                    }
                                )
                                IconButton(
                                    onClick = { showMorePopup.value = true },
                                    holdDownState = showMorePopup.value,
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.MoreCircle,
                                        tint = MiuixTheme.colorScheme.onSurface,
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    },
                    bottomContent = {
                        if (selectedIndex == 1) {
                            Box(
                                modifier = Modifier
                                    .alpha(if (searchStatus.isCollapsed()) 1f else 0f)
                                    .onGloballyPositioned { coordinates ->
                                        with(density) {
                                            val newOffsetY = coordinates.positionInWindow().y.toDp()
                                            if (searchStatus.offsetY != newOffsetY) {
                                                searchStatus = searchStatus.copy(offsetY = newOffsetY)
                                            }
                                        }
                                    }
                                    .then(
                                        if (searchStatus.isCollapsed()) {
                                            Modifier.pointerInput(Unit) {
                                                detectTapGestures {
                                                    searchStatus = searchStatus.copy(current = SearchStatus.Status.EXPANDING)
                                                }
                                            }
                                        } else Modifier
                                    )
                            ) {
                                SearchBarFake(searchStatus.label, dynamicTopPadding)
                            }
                        }
                    }
                )
            }
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
        },
        popupHost = {
            if (selectedIndex == 1) {
                searchStatus.SearchPager(
                    onSearchStatusChange = { searchStatus = it },
                    defaultResult = {
                        // 显示默认的应用列表
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().overScrollVertical()
                        ) {
                            items(searchResults, key = { it.packageName }) { pi ->
                                MiuixAppItem(
                                    packageInfo = pi,
                                    isGranted = false,
                                    onToggle = { }
                                )
                            }
                        }
                    },
                    searchBarTopPadding = dynamicTopPadding,
                ) {
                    // 显示搜索结果
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().overScrollVertical()
                    ) {
                        items(searchResults, key = { it.packageName }) { pi ->
                            MiuixAppItem(
                                packageInfo = pi,
                                isGranted = false,
                                onToggle = { }
                            )
                        }
                    }
                }
            }
        },
        containerColor = MiuixTheme.colorScheme.surface,
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
                    scrollBehavior = scrollBehavior,
                    sortOption = sortOption,
                    showSystemApps = showSystemApps,
                    searchStatus = searchStatus,
                    onSearchStatusChange = { searchStatus = it },
                    onSearchResults = { searchResults = it }
                )
                2 -> MiuixSettingsScreen(
                    scrollBehavior = scrollBehavior
                )
            }
        }
    }
}
