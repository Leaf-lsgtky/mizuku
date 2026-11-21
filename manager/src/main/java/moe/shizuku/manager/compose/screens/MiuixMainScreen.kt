package moe.shizuku.manager.compose.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.shizuku.manager.R
import moe.shizuku.manager.compose.components.FloatingBottomBar
import moe.shizuku.manager.compose.components.FloatingBottomBarItem
import moe.shizuku.manager.compose.components.miuix.LocalPageBackdrop
import moe.shizuku.manager.compose.components.miuix.ContentSizedGlassNavigationBar
import moe.shizuku.manager.compose.components.miuix.isOs4WideWindow
import moe.shizuku.manager.compose.rememberMainPagerState
import moe.shizuku.manager.compose.theme.LocalThemeState
import moe.shizuku.manager.home.HomeViewModel
import moe.shizuku.manager.management.AppsViewModel
import moe.shizuku.manager.utils.ShizukuStateMachine
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.glass.GlassDialog
import top.yukonga.miuix.kmp.glass.GlassDialogButton
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassIconDropdownMenu
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchorContent
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.glass.rememberGlassSearchBarAlpha
import top.yukonga.miuix.kmp.glass.glassSearchBarTransition
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.All
import top.yukonga.miuix.kmp.icon.glass.HomeOutline
import top.yukonga.miuix.kmp.icon.glass.More
import top.yukonga.miuix.kmp.icon.glass.Search
import top.yukonga.miuix.kmp.icon.glass.SettingsOutline
import top.yukonga.miuix.kmp.icon.glass.Sort
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixMainScreen(
    homeViewModel: HomeViewModel,
    appsViewModel: AppsViewModel,
    onNavigateToStarter: (isRoot: Boolean, port: Int) -> Unit,
    onNavigateToShellTutorial: () -> Unit,
    onNavigateToAdbPairingTutorial: () -> Unit,
    onNavigateToAppearance: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val mainPagerState = rememberMainPagerState(pagerState)
    val homeScrollBehavior = MiuixScrollBehavior()
    val appsScrollBehavior = MiuixScrollBehavior()
    val settingsScrollBehavior = MiuixScrollBehavior()
    var sortOption by rememberSaveable { mutableIntStateOf(0) }
    var showSystemApps by rememberSaveable { mutableStateOf(false) }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    val searchBarAlpha = rememberGlassSearchBarAlpha(searchExpanded)
    var showStopDialog by remember { mutableStateOf(false) }
    var showTopPopup by remember { mutableStateOf(false) }
    var showSortPopup by remember { mutableStateOf(false) }
    var showMorePopup by remember { mutableStateOf(false) }
    val topMenuAnchor = rememberGlassPopupAnchor()
    val sortMenuAnchor = rememberGlassPopupAnchor()
    val moreMenuAnchor = rememberGlassPopupAnchor()

    LaunchedEffect(pagerState.currentPage, mainPagerState.isNavigating) {
        mainPagerState.syncPage()
    }
    LaunchedEffect(mainPagerState.selectedPage) {
        if (mainPagerState.selectedPage != 1) searchExpanded = false
        showTopPopup = false
        showSortPopup = false
        showMorePopup = false
    }
    BackHandler(enabled = searchExpanded || mainPagerState.selectedPage != 0) {
        if (searchExpanded) searchExpanded = false
        else mainPagerState.animateToPage(0)
    }

    val themeState = LocalThemeState.current
    val effectsEnabled = themeState.enableBlur && isRuntimeShaderSupported()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val pageBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val navigationBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val destinations = listOf(
        GlassNavigationItem(MiuixIcons.Glass.HomeOutline, stringResource(R.string.app_name)),
        GlassNavigationItem(MiuixIcons.Glass.All, stringResource(R.string.navigation_apps), stringResource(R.string.home_app_management_title)),
        GlassNavigationItem(MiuixIcons.Glass.SettingsOutline, stringResource(R.string.settings_title)),
    )

    // The outer host renders dialogs outside both recordings, including their scrims.
    CompositionLocalProvider(LocalPageBackdrop provides pageBackdrop.takeIf { effectsEnabled }) {
        Scaffold(contentWindowInsets = WindowInsets(0)) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.fillMaxSize().then(
                        if (effectsEnabled) Modifier.layerBackdrop(pageBackdrop) else Modifier,
                    ),
                ) {
                    Scaffold(
                        popupHost = {},
                        contentWindowInsets = WindowInsets(0),
                        containerColor = Color.Transparent,
                        bottomBar = {
                            Box(
                                Modifier.fillMaxWidth()
                                    .graphicsLayer { alpha = searchBarAlpha.value }
                                    .layout { measurable, constraints ->
                                        val bar = measurable.measure(constraints)
                                        layout(bar.width, bar.height) {
                                            // Keep page padding stable, but let search results receive input after the bar fades.
                                            if (searchBarAlpha.value > 0f) bar.placeRelative(0, 0)
                                        }
                                    }
                                    .then(if (searchExpanded) Modifier.clearAndSetSemantics { } else Modifier),
                            ) {
                                if (themeState.enableFloatingBottomBar) {
                                    Box(Modifier.fillMaxWidth()) {
                                        FloatingBottomBar(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(horizontal = 12.dp)
                                                .windowInsetsPadding(
                                                    WindowInsets.displayCutout.union(WindowInsets.navigationBars)
                                                        .only(WindowInsetsSides.Horizontal),
                                                )
                                                .navigationBarsPadding()
                                                .padding(bottom = 12.dp),
                                            selectedIndex = { mainPagerState.selectedPage },
                                            onSelected = { if (!searchExpanded) mainPagerState.animateToPage(it) },
                                            backdrop = navigationBackdrop,
                                            tabsCount = destinations.size,
                                            isBlurEnabled = effectsEnabled && themeState.enableFloatingBottomBarBlur,
                                        ) {
                                            destinations.forEachIndexed { index, destination ->
                                                FloatingBottomBarItem(
                                                    onClick = { if (!searchExpanded) mainPagerState.animateToPage(index) },
                                                    modifier = Modifier.defaultMinSize(minWidth = 76.dp),
                                                    selected = mainPagerState.selectedPage == index,
                                                ) {
                                                    Icon(destination.icon, null, Modifier.size(22.dp))
                                                    Text(
                                                        text = destination.label.orEmpty(),
                                                        style = MiuixTheme.textStyles.footnote2,
                                                        fontSize = 11.sp,
                                                        lineHeight = 14.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    ContentSizedGlassNavigationBar(
                                        items = destinations,
                                        selectedIndex = mainPagerState.selectedPage,
                                        onSelect = { if (!searchExpanded) mainPagerState.animateToPage(it) },
                                        backdrop = navigationBackdrop.takeIf { effectsEnabled },
                                    )
                                }
                            }
                        },
                    ) { shellPadding ->
                        Box(
                            Modifier.fillMaxSize().then(
                                if (effectsEnabled) Modifier.layerBackdrop(navigationBackdrop) else Modifier,
                            ),
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                userScrollEnabled = !searchExpanded,
                                beyondViewportPageCount = 1,
                                overscrollEffect = null,
                                modifier = Modifier.fillMaxSize(),
                            ) { page ->
                                when (page) {
                                    0 -> MiuixMainPageWrapper(
                                        title = destinations[0].label.orEmpty(),
                                        enableBlur = effectsEnabled,
                                        bottomPadding = shellPadding.calculateBottomPadding(),
                                        scrollBehavior = homeScrollBehavior,
                                        actions = {
                                            GlassIconButton(
                                                onClick = { showTopPopup = true },
                                                modifier = Modifier.glassPopupAnchor(topMenuAnchor, cornerRadius = GlassTopAppBarDefaults.ButtonSize / 2, floating = true),
                                            ) {
                                                Icon(
                                                    MiuixIcons.Glass.More,
                                                    stringResource(R.string.settings_title),
                                                    Modifier.size(28.dp).glassPopupAnchorContent(topMenuAnchor),
                                                )
                                            }
                                        },
                                    ) { padding ->
                                        MiuixHomeScreen(
                                            homeViewModel = homeViewModel,
                                            appsViewModel = appsViewModel,
                                            onNavigateToStarter = onNavigateToStarter,
                                            onNavigateToShellTutorial = onNavigateToShellTutorial,
                                            onNavigateToAdbPairingTutorial = onNavigateToAdbPairingTutorial,
                                            onNavigateToApps = { mainPagerState.animateToPage(1) },
                                            scrollBehavior = homeScrollBehavior,
                                            scaffoldPadding = padding,
                                        )
                                    }
                                    1 -> MiuixAppManagementScreen(
                                        viewModel = appsViewModel,
                                        scrollBehavior = appsScrollBehavior,
                                        sortOption = sortOption,
                                        showSystemApps = showSystemApps,
                                        searchExpanded = searchExpanded,
                                        onSearchExpandedChange = { searchExpanded = it },
                                        pageContent = { listContent ->
                                            MiuixMainPageWrapper(
                                                title = stringResource(R.string.home_app_management_title),
                                                modifier = Modifier
                                                    .graphicsLayer { alpha = searchBarAlpha.value }
                                                    .then(if (searchExpanded) Modifier.clearAndSetSemantics { } else Modifier),
                                                topBarContentModifier = Modifier.glassSearchBarTransition(searchExpanded, blurEnabled = effectsEnabled) { 1f },
                                                enableBlur = effectsEnabled,
                                                bottomPadding = shellPadding.calculateBottomPadding(),
                                                scrollBehavior = appsScrollBehavior,
                                                actions = {
                                                    GlassIconButton(onClick = { searchExpanded = true }) {
                                                        Icon(MiuixIcons.Glass.Search, stringResource(R.string.search_apps), Modifier.size(28.dp))
                                                    }
                                                    GlassIconButton(
                                                        onClick = { showSortPopup = true },
                                                        modifier = Modifier.glassPopupAnchor(sortMenuAnchor, cornerRadius = GlassTopAppBarDefaults.ButtonSize / 2, floating = true),
                                                    ) {
                                                        Icon(MiuixIcons.Glass.Sort, stringResource(R.string.menu_sort), Modifier.size(28.dp).glassPopupAnchorContent(sortMenuAnchor))
                                                    }
                                                    GlassIconButton(
                                                        onClick = { showMorePopup = true },
                                                        modifier = Modifier.glassPopupAnchor(moreMenuAnchor, cornerRadius = GlassTopAppBarDefaults.ButtonSize / 2, floating = true),
                                                    ) {
                                                        Icon(MiuixIcons.Glass.More, stringResource(R.string.settings_title), Modifier.size(28.dp).glassPopupAnchorContent(moreMenuAnchor))
                                                    }
                                                },
                                                content = listContent,
                                            )
                                        },
                                    )
                                    2 -> MiuixMainPageWrapper(
                                        title = destinations[2].label.orEmpty(),
                                        enableBlur = effectsEnabled,
                                        bottomPadding = shellPadding.calculateBottomPadding(),
                                        scrollBehavior = settingsScrollBehavior,
                                    ) { padding ->
                                        MiuixSettingsScreen(
                                            scrollBehavior = settingsScrollBehavior,
                                            scaffoldPadding = padding,
                                            onNavigateToAppearance = onNavigateToAppearance,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                GlassIconDropdownMenu(
                    show = showTopPopup,
                    onDismissRequest = { showTopPopup = false },
                    anchor = topMenuAnchor,
                    anchorContent = { Icon(MiuixIcons.Glass.More, null, Modifier.size(28.dp)) },
                    entries = listOf(DropdownEntry(items = listOf(DropdownItem(
                        text = stringResource(R.string.action_stop),
                        onClick = { showStopDialog = true },
                    )))),
                    backdrop = LocalPageBackdrop.current,
                )
                GlassIconDropdownMenu(
                    show = showSortPopup,
                    onDismissRequest = { showSortPopup = false },
                    anchor = sortMenuAnchor,
                    anchorContent = { Icon(MiuixIcons.Glass.Sort, null, Modifier.size(28.dp)) },
                    entries = listOf(DropdownEntry(items = listOf(
                        DropdownItem(text = stringResource(R.string.sort_by_name), selected = sortOption == 0, onClick = { sortOption = 0 }),
                        DropdownItem(text = stringResource(R.string.sort_by_package_name), selected = sortOption == 1, onClick = { sortOption = 1 }),
                    ))),
                    backdrop = LocalPageBackdrop.current,
                )
                GlassIconDropdownMenu(
                    show = showMorePopup,
                    onDismissRequest = { showMorePopup = false },
                    anchor = moreMenuAnchor,
                    anchorContent = { Icon(MiuixIcons.Glass.More, null, Modifier.size(28.dp)) },
                    entries = listOf(DropdownEntry(items = listOf(DropdownItem(
                        text = stringResource(R.string.show_system_apps),
                        selected = showSystemApps,
                        onClick = { showSystemApps = !showSystemApps },
                    )))),
                    backdrop = LocalPageBackdrop.current,
                )
                GlassDialog(
                    show = showStopDialog,
                    onDismissRequest = { showStopDialog = false },
                    title = stringResource(R.string.app_name),
                    backdrop = LocalPageBackdrop.current,
                ) {
                    Column {
                        Text(stringResource(R.string.dialog_stop_message), style = MiuixTheme.textStyles.body2)
                        Spacer(Modifier.height(24.dp))
                        Row(Modifier.fillMaxWidth()) {
                            GlassDialogButton(onClick = { showStopDialog = false }, modifier = Modifier.weight(1f)) {
                                Text(stringResource(android.R.string.cancel), style = MiuixTheme.textStyles.button)
                            }
                            Spacer(Modifier.width(20.dp))
                            GlassDialogButton(
                                onClick = {
                                    ShizukuStateMachine.set(ShizukuStateMachine.State.STOPPING)
                                    runCatching { rikka.shizuku.Shizuku.exit() }
                                    showStopDialog = false
                                },
                                modifier = Modifier.weight(1f),
                                role = GlassDialogButtonRole.Destructive,
                            ) { Text(stringResource(android.R.string.ok), style = MiuixTheme.textStyles.button) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiuixMainPageWrapper(
    title: String,
    modifier: Modifier = Modifier,
    topBarContentModifier: Modifier = Modifier,
    enableBlur: Boolean,
    bottomPadding: Dp,
    scrollBehavior: ScrollBehavior,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            GlassTopAppBar(
                title = title,
                contentModifier = topBarContentModifier,
                expandable = !isOs4WideWindow(),
                backdrop = backdrop.takeIf { enableBlur },
                progressiveBlur = enableBlur,
                titleBlurRadius = if (enableBlur) GlassTopAppBarDefaults.TitleBlurRadius else 0.dp,
                scrollBehavior = scrollBehavior,
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions,
                    )
                },
            )
        },
        popupHost = {},
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Box(
            Modifier.fillMaxSize()
                .then(if (enableBlur) Modifier.layerBackdrop(backdrop) else Modifier)
                .windowInsetsPadding(
                    WindowInsets.displayCutout.union(WindowInsets.navigationBars).only(WindowInsetsSides.Horizontal),
                ),
        ) {
            content(PaddingValues(top = padding.calculateTopPadding(), bottom = bottomPadding))
        }
    }
}
