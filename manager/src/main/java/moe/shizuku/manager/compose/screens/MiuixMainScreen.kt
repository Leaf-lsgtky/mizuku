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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.compose.MainPagerState
import moe.shizuku.manager.compose.components.SearchBarFake
import moe.shizuku.manager.compose.components.SearchPager
import moe.shizuku.manager.compose.components.SearchStatus
import moe.shizuku.manager.compose.rememberMainPagerState
import moe.shizuku.manager.compose.utils.BlurredBar
import moe.shizuku.manager.compose.utils.rememberBlurBackdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import moe.shizuku.manager.home.HomeViewModel
import moe.shizuku.manager.management.AppsViewModel
import moe.shizuku.manager.utils.ShizukuStateMachine
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
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
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val mainPagerState = rememberMainPagerState(pagerState)
    val scrollBehavior = MiuixScrollBehavior()
    val density = LocalDensity.current

    // 同步页面状态
    LaunchedEffect(pagerState.settledPage) {
        mainPagerState.syncPage()
    }

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

    // 模糊效果
    val enableBlur = ShizukuSettings.getEnableBlur()
    val blurBackdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = blurBackdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

    // 返回按钮处理：取消搜索或返回首页
    BackHandler(enabled = !searchStatus.isCollapsed() || mainPagerState.selectedPage != 0) {
        if (!searchStatus.isCollapsed()) {
            searchStatus = searchStatus.copy(
                searchText = "",
                current = SearchStatus.Status.COLLAPSING
            )
        } else if (mainPagerState.selectedPage != 0) {
            mainPagerState.animateToPage(0)
        }
    }

    if (showStopDialog) {
        WindowDialog(
            show = true,
            onDismissRequest = { showStopDialog = false },
            title = stringResource(R.string.app_name),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.dialog_stop_message),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(24.dp))
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = { showStopDialog = false },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    TextButton(
                        text = stringResource(android.R.string.ok),
                        onClick = {
                            ShizukuStateMachine.set(ShizukuStateMachine.State.STOPPING)
                            runCatching { rikka.shizuku.Shizuku.exit() }
                            showStopDialog = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
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
        bottomBar = {
            BlurredBar(blurBackdrop) {
                NavigationBar(
                    color = barColor
                ) {
                    NavigationBarItem(
                        selected = mainPagerState.selectedPage == 0,
                        onClick = { mainPagerState.animateToPage(0) },
                        icon = MiuixIcons.VerticalSplit,
                        label = stringResource(R.string.app_name)
                    )
                    NavigationBarItem(
                        selected = mainPagerState.selectedPage == 1,
                        onClick = { mainPagerState.animateToPage(1) },
                        icon = MiuixIcons.All,
                        label = stringResource(R.string.home_app_management_title)
                    )
                    NavigationBarItem(
                        selected = mainPagerState.selectedPage == 2,
                        onClick = { mainPagerState.animateToPage(2) },
                        icon = MiuixIcons.Settings,
                        label = stringResource(R.string.settings_title)
                    )
                }
            }
        },
        containerColor = MiuixTheme.colorScheme.surface,
    ) { paddingValues ->
        HorizontalPager(
            state = mainPagerState.pagerState,
            userScrollEnabled = true,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .then(
                    if (blurActive) Modifier.layerBackdrop(blurBackdrop)
                    else Modifier
                )
        ) { page ->
            when (page) {
                0 -> MiuixMainPageWrapper(
                    title = pages[0],
                    barColor = barColor,
                    blurBackdrop = blurBackdrop,
                    scrollBehavior = scrollBehavior,
                    actions = {
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
                ) {
                    MiuixHomeScreen(
                        homeViewModel = homeViewModel,
                        appsViewModel = appsViewModel,
                        onNavigateToStarter = onNavigateToStarter,
                        onNavigateToShellTutorial = onNavigateToShellTutorial,
                        onNavigateToAdbPairingTutorial = onNavigateToAdbPairingTutorial,
                        scrollBehavior = scrollBehavior
                    )
                }
                1 -> MiuixMainPageWrapper(
                    title = pages[1],
                    barColor = barColor,
                    blurBackdrop = blurBackdrop,
                    scrollBehavior = scrollBehavior,
                    actions = {
                        if (searchStatus.isCollapsed()) {
                            // 排序按钮
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

                            // 更多选项按钮
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
                ) {
                    MiuixAppManagementScreen(
                        viewModel = appsViewModel,
                        scrollBehavior = scrollBehavior,
                        sortOption = sortOption,
                        showSystemApps = showSystemApps,
                        searchStatus = searchStatus,
                        onSearchStatusChange = { searchStatus = it },
                        onSearchResults = { searchResults = it }
                    )
                }
                2 -> MiuixMainPageWrapper(
                    title = pages[2],
                    barColor = barColor,
                    blurBackdrop = blurBackdrop,
                    scrollBehavior = scrollBehavior,
                ) {
                    MiuixSettingsScreen(
                        scrollBehavior = scrollBehavior
                    )
                }
            }
        }
    }

    if (mainPagerState.selectedPage == 1) {
        searchStatus.SearchPager(
            onSearchStatusChange = { searchStatus = it },
            defaultResult = {
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
}

@Composable
private fun MiuixMainPageWrapper(
    title: String,
    barColor: Color,
    blurBackdrop: LayerBackdrop?,
    scrollBehavior: ScrollBehavior,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            BlurredBar(blurBackdrop) {
                TopAppBar(
                    title = title,
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                    actions = actions,
                    bottomContent = bottomContent,
                )
            }
        },
        popupHost = { },
    ) {
        content()
    }
}
