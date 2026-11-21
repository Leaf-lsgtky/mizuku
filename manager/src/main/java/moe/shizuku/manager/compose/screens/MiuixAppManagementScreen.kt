package moe.shizuku.manager.compose.screens

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import top.yukonga.miuix.kmp.glass.GlassSearchOverlay
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.Search
import top.yukonga.miuix.kmp.icon.glass.Close
import top.yukonga.miuix.kmp.glass.GlassSearchOverlayDefaults
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import moe.shizuku.manager.Helps
import moe.shizuku.manager.R
import moe.shizuku.manager.authorization.AuthorizationManager
import moe.shizuku.manager.compose.components.miuix.LocalPageBackdrop
import moe.shizuku.manager.ktx.toHtml
import moe.shizuku.manager.utils.AppIconCache
import moe.shizuku.manager.utils.ShizukuStateMachine
import moe.shizuku.manager.utils.ShizukuSystemApis
import moe.shizuku.manager.utils.UserHandleCompat
import rikka.html.text.HtmlCompat
import rikka.lifecycle.Status
import rikka.shizuku.Shizuku
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.glass.GlassDialog
import top.yukonga.miuix.kmp.glass.GlassDialogButton
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixAppManagementScreen(
    viewModel: moe.shizuku.manager.management.AppsViewModel,
    scrollBehavior: top.yukonga.miuix.kmp.basic.ScrollBehavior,
    sortOption: Int = 0,
    showSystemApps: Boolean = false,
    searchExpanded: Boolean = false,
    onSearchExpandedChange: (Boolean) -> Unit = {},
    scaffoldPadding: PaddingValues = PaddingValues(0.dp),
    pageContent: @Composable (@Composable (PaddingValues) -> Unit) -> Unit = { content ->
        content(scaffoldPadding)
    },
) {
    val packagesResource by viewModel.packages.observeAsState()
    val context = LocalContext.current
    val grantedStates = remember { mutableStateMapOf<String, Boolean>() }
    var isRefreshing by remember { mutableStateOf(false) }
    var showBulkAdbLimitedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!ShizukuStateMachine.isRunning()) {
            return@LaunchedEffect
        }
        viewModel.load()
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            viewModel.load()
            kotlinx.coroutines.delay(1000)
            isRefreshing = false
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        packagesResource?.data?.forEach { pi ->
            val uid = pi.applicationInfo?.uid ?: return@forEach
            grantedStates["${pi.packageName}:${pi.applicationInfo?.uid}"] = AuthorizationManager.granted(pi.packageName, uid)
        }
    }

    LaunchedEffect(packagesResource) {
        if (packagesResource?.status == Status.SUCCESS) {
            packagesResource?.data?.forEach { pi ->
                val uid = pi.applicationInfo?.uid ?: return@forEach
                grantedStates["${pi.packageName}:${pi.applicationInfo?.uid}"] = AuthorizationManager.granted(pi.packageName, uid)
            }
        }
    }

    val packages = packagesResource?.data
    val isLoading = packagesResource?.status == Status.LOADING
    val isError = packagesResource?.status == Status.ERROR

    fun filterAndSort(query: String): List<PackageInfo>? = packages?.let { list ->
        val filtered = if (showSystemApps) list else list.filter { pi ->
            val info = pi.applicationInfo
            info != null && (info.flags and ApplicationInfo.FLAG_SYSTEM) == 0
        }
        val searched = if (query.isBlank()) filtered else filtered.filter { pi ->
            val label = pi.applicationInfo?.loadLabel(context.packageManager)?.toString() ?: pi.packageName
            label.contains(query, ignoreCase = true) || pi.packageName.contains(query, ignoreCase = true)
        }
        when (sortOption) {
            0 -> searched.sortedBy { it.applicationInfo?.loadLabel(context.packageManager)?.toString()?.lowercase() ?: it.packageName.lowercase() }
            1 -> searched.sortedBy { it.packageName.lowercase() }
            else -> searched
        }
    }

    val filteredAndSortedPackages = remember(packages, sortOption, showSystemApps) { filterAndSort("") }
    val onToggle: (PackageInfo, Boolean) -> Unit = { pi, granted ->
        val uid = pi.applicationInfo?.uid
        if (uid != null) {
            if (granted) AuthorizationManager.grant(pi.packageName, uid)
            else AuthorizationManager.revoke(pi.packageName, uid)
            grantedStates["${pi.packageName}:${pi.applicationInfo?.uid}"] = granted
        }
    }

    fun setAllGranted(grant: Boolean) {
        filteredAndSortedPackages?.forEach { pi ->
            try {
                onToggle(pi, grant)
            } catch (_: SecurityException) {
                showBulkAdbLimitedDialog = true
            }
        }
    }

    MiuixAdbLimitedDialog(showBulkAdbLimitedDialog) { showBulkAdbLimitedDialog = false }
    Box(Modifier.fillMaxSize()) {
        pageContent { contentPadding ->
            val topContentPadding = contentPadding.calculateTopPadding()
            val bottomContentPadding = contentPadding.calculateBottomPadding()
            val onOverscrollChanged: (Float) -> Unit = remember(scrollBehavior.state) {
                { scrollBehavior.state.overscrollOffset = it }
            }
            val lazyListState = rememberLazyListState()
            if (isLoading && filteredAndSortedPackages.isNullOrEmpty() && !isRefreshing) {
                Box(Modifier.fillMaxSize().padding(top = topContentPadding, bottom = bottomContentPadding), contentAlignment = Alignment.Center) {
                    InfiniteProgressIndicator()
                }
            } else if (isError && filteredAndSortedPackages.isNullOrEmpty()) {
                Box(Modifier.fillMaxSize().padding(top = topContentPadding, bottom = bottomContentPadding), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.home_app_management_empty),
                        modifier = Modifier.padding(horizontal = 24.dp),
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                val pullToRefreshState = rememberPullToRefreshState()
                val refreshTexts = listOf(stringResource(R.string.refresh_pulling), stringResource(R.string.refresh_release), stringResource(R.string.refresh_refresh), stringResource(R.string.refresh_complete))
                PullToRefresh(
                    isRefreshing = isRefreshing,
                    pullToRefreshState = pullToRefreshState,
                    onRefresh = { if (!isLoading && !isRefreshing) isRefreshing = true },
                    refreshTexts = refreshTexts,
                    contentPadding = PaddingValues(top = topContentPadding + 6.dp),
                ) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxHeight().scrollEndHaptic().overScrollVertical(onOffsetChanged = onOverscrollChanged).nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(top = topContentPadding + 6.dp, bottom = bottomContentPadding + 16.dp),
                        overscrollEffect = null,
                    ) {
                        if (!filteredAndSortedPackages.isNullOrEmpty()) item {
                            val allGranted = filteredAndSortedPackages?.all { pi ->
                                val uid = pi.applicationInfo?.uid ?: return@all true
                                grantedStates["${pi.packageName}:${pi.applicationInfo?.uid}"] ?: AuthorizationManager.granted(pi.packageName, uid)
                            } ?: false
                            Card(Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth()) {
                                BasicComponent(
                                    title = stringResource(R.string.app_management_toggle_all),
                                    endActions = {
                                        Switch(
                                            checked = allGranted,
                                            enabled = !filteredAndSortedPackages.isNullOrEmpty(),
                                            onCheckedChange = ::setAllGranted,
                                        )
                                    },
                                    onClick = { setAllGranted(!allGranted) },
                                )
                            }
                        }
                        if (!filteredAndSortedPackages.isNullOrEmpty()) {
                            appManagementItems(filteredAndSortedPackages, grantedStates, onToggle)
                        } else {
                            item {
                                Text(
                                    stringResource(R.string.home_app_management_empty),
                                    modifier = Modifier.padding(24.dp),
                                    style = MiuixTheme.textStyles.body1,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        }
                    }
                }
            }
        }

        val searchListState = rememberLazyListState()
        val searchTopInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top)
        val horizontalInsets = WindowInsets.displayCutout.union(WindowInsets.navigationBars).only(WindowInsetsSides.Horizontal)
        val searchTopPadding = searchTopInsets.asPaddingValues().calculateTopPadding() +
            GlassSearchOverlayDefaults.BarHeight + GlassSearchOverlayDefaults.VerticalPadding * 2 + 12.dp
        val searchBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
        GlassSearchOverlay(
            expanded = searchExpanded,
            onDismissRequest = { onSearchExpandedChange(false) },
            label = stringResource(R.string.search_apps),
            searchIcon = MiuixIcons.Glass.Search,
            closeIcon = MiuixIcons.Glass.Close,
            topInsets = searchTopInsets,
            horizontalInsetsModifier = Modifier.windowInsetsPadding(horizontalInsets),
            blurEnabled = LocalPageBackdrop.current != null,
            scrollOffset = {
                if (searchListState.firstVisibleItemIndex > 0) Float.MAX_VALUE
                else searchListState.firstVisibleItemScrollOffset.toFloat()
            },
        ) { query ->
            val results = remember(filteredAndSortedPackages, query) {
                filteredAndSortedPackages?.filter { pi ->
                    val label = pi.applicationInfo?.loadLabel(context.packageManager)?.toString() ?: pi.packageName
                    query.isBlank() || label.contains(query, ignoreCase = true) || pi.packageName.contains(query, ignoreCase = true)
                }
            }
            LazyColumn(
                state = searchListState,
                modifier = Modifier.fillMaxSize()
                    .windowInsetsPadding(horizontalInsets)
                    .overScrollVertical(),
                contentPadding = PaddingValues(top = searchTopPadding, bottom = searchBottomPadding),
                overscrollEffect = null,
            ) {
                if (results.isNullOrEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            if (isLoading) InfiniteProgressIndicator()
                            else Text(stringResource(if (query.isBlank()) R.string.home_app_management_empty else R.string.search_apps_no_results), style = MiuixTheme.textStyles.body1)
                        }
                    }
                } else {
                    appManagementItems(results, grantedStates, onToggle)
                }
            }
        }
    }
}

private fun LazyListScope.appManagementItems(
    packages: List<PackageInfo>,
    grantedStates: Map<String, Boolean>,
    onToggle: (PackageInfo, Boolean) -> Unit,
) {
    items(packages, key = { "${it.packageName}:${it.applicationInfo?.uid}" }) { pi ->
        MiuixAppItem(
            packageInfo = pi,
            isGranted = grantedStates["${pi.packageName}:${pi.applicationInfo?.uid}"] ?: false,
            onToggle = { granted -> onToggle(pi, granted) },
        )
    }
}

@Composable
fun MiuixAppItem(
    packageInfo: PackageInfo,
    isGranted: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val appInfo = packageInfo.applicationInfo
    val uid = appInfo?.uid ?: 0
    val userId = UserHandleCompat.getUserId(uid)
    val isOtherUser = userId != UserHandleCompat.myUserId()
    val requiresRoot = remember(packageInfo) {
        appInfo?.metaData?.getBoolean("moe.shizuku.client.V3_REQUIRES_ROOT") ?: false
    }

    var showAdbLimitedDialog by remember { mutableStateOf(false) }

    val label = remember(packageInfo) {
        try {
            val baseLabel = appInfo?.loadLabel(context.packageManager) ?: packageInfo.packageName
            if (isOtherUser) {
                try {
                    val userInfo = ShizukuSystemApis.getUserInfo(userId)
                    "$baseLabel - ${userInfo.name} ($userId)"
                } catch (e: Exception) {
                    "$baseLabel ($userId)"
                }
            } else {
                baseLabel
            }
        } catch (e: Exception) {
            packageInfo.packageName
        }
    }
    val iconBitmap = remember(packageInfo) {
        val size = context.resources.getDimensionPixelSize(R.dimen.default_app_icon_size)
        appInfo?.let { AppIconCache.getOrLoadBitmap(context, it, userId, size) }
    }

    MiuixAdbLimitedDialog(
        show = showAdbLimitedDialog,
        onDismiss = { showAdbLimitedDialog = false },
    )

    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth(),
        onClick = {
            try {
                onToggle(!isGranted)
            } catch (e: SecurityException) {
                try {
                    val currentUid = Shizuku.getUid()
                    if (currentUid != 0) {
                        showAdbLimitedDialog = true
                    }
                } catch (ex: Throwable) {
                    // Ignore
                }
            }
        },
        showIndication = true,
        insideMargin = PaddingValues(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 应用图标
            Box(
                modifier = Modifier
                    .padding(end = 10.dp)
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp)),
            ) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // 应用信息
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = label.toString(),
                    modifier = Modifier,
                    fontWeight = FontWeight(550),
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = packageInfo.packageName,
                    modifier = Modifier,
                    fontSize = 12.sp,
                    fontWeight = FontWeight(550),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    softWrap = false
                )
                if (requiresRoot) {
                    Text(
                        text = stringResource(R.string.app_management_item_summary_requires_root),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.error,
                    )
                }
            }

            // 开关
            Switch(
                checked = isGranted,
                onCheckedChange = { granted ->
                    try {
                        onToggle(granted)
                    } catch (e: SecurityException) {
                        try {
                            val currentUid = Shizuku.getUid()
                            if (currentUid != 0) {
                                showAdbLimitedDialog = true
                            }
                        } catch (ex: Throwable) {
                            // Ignore
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun MiuixAdbLimitedDialog(show: Boolean, onDismiss: () -> Unit) {
    val textColor = MiuixTheme.colorScheme.onSurface.toArgb()

    GlassDialog(
        show = show,
        backdrop = LocalPageBackdrop.current,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.app_management_dialog_adb_is_limited_title),
    ) {
        Column {
            AndroidView(
                factory = { ctx ->
                    TextView(ctx).apply {
                        movementMethod = LinkMovementMethod.getInstance()
                        setTextColor(textColor)
                        text = ctx.getString(
                            R.string.app_management_dialog_adb_is_limited_message,
                            Helps.ADB.get()
                        ).toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
                    }
                },
                update = { tv ->
                    tv.setTextColor(textColor)
                    tv.text = tv.context.getString(
                        R.string.app_management_dialog_adb_is_limited_message,
                        Helps.ADB.get()
                    ).toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
                },
            )
            Spacer(modifier = Modifier.height(24.dp))
            GlassDialogButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                role = GlassDialogButtonRole.Primary,
            ) {
                Text(
                    text = stringResource(android.R.string.ok),
                    style = MiuixTheme.textStyles.button,
                )
            }
        }
    }
}
