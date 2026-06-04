package moe.shizuku.manager.compose.screens

import android.content.pm.PackageInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import moe.shizuku.manager.Helps
import moe.shizuku.manager.R
import moe.shizuku.manager.authorization.AuthorizationManager
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
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun MiuixAppManagementScreen(
    viewModel: moe.shizuku.manager.management.AppsViewModel,
    scrollBehavior: top.yukonga.miuix.kmp.basic.ScrollBehavior,
) {
    val packagesResource by viewModel.packages.observeAsState()
    val context = LocalContext.current
    val grantedStates = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(Unit) {
        if (!ShizukuStateMachine.isRunning()) {
            return@LaunchedEffect
        }
        viewModel.load()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        packagesResource?.data?.forEach { pi ->
            val uid = pi.applicationInfo?.uid ?: return@forEach
            grantedStates[pi.packageName] = AuthorizationManager.granted(pi.packageName, uid)
        }
    }

    LaunchedEffect(packagesResource) {
        if (packagesResource?.status == Status.SUCCESS) {
            packagesResource?.data?.forEach { pi ->
                val uid = pi.applicationInfo?.uid ?: return@forEach
                grantedStates[pi.packageName] = AuthorizationManager.granted(pi.packageName, uid)
            }
        }
    }

    val packages = packagesResource?.data
    val isLoading = packagesResource?.status == Status.LOADING
    val isError = packagesResource?.status == Status.ERROR

    if (isLoading && packages.isNullOrEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            InfiniteProgressIndicator()
        }
    } else if (isError && packages.isNullOrEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.home_app_management_empty),
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    } else {
        val pullToRefreshState = rememberPullToRefreshState()
        val refreshTexts = listOf(
            stringResource(R.string.refresh_pulling),
            stringResource(R.string.refresh_release),
            stringResource(R.string.refresh_refresh),
            stringResource(R.string.refresh_complete),
        )

        PullToRefresh(
            isRefreshing = isLoading,
            pullToRefreshState = pullToRefreshState,
            onRefresh = { viewModel.load() },
            refreshTexts = refreshTexts,
            contentPadding = PaddingValues(vertical = 6.dp),
        ) {
            val lazyListState = rememberLazyListState()
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxHeight()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(vertical = 6.dp),
                overscrollEffect = null,
            ) {
                // 全部切换按钮
                item {
                    val allGranted = packages?.all { pi ->
                        val uid = pi.applicationInfo?.uid ?: return@all true
                        grantedStates[pi.packageName] ?: AuthorizationManager.granted(pi.packageName, uid)
                    } ?: false

                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .fillMaxWidth(),
                    ) {
                        BasicComponent(
                            title = stringResource(R.string.app_management_toggle_all),
                            endActions = {
                                Switch(
                                    checked = allGranted,
                                    onCheckedChange = { grant ->
                                        packages?.forEach { pi ->
                                            val uid = pi.applicationInfo?.uid ?: return@forEach
                                            if (grant) {
                                                AuthorizationManager.grant(pi.packageName, uid)
                                            } else {
                                                AuthorizationManager.revoke(pi.packageName, uid)
                                            }
                                            grantedStates[pi.packageName] = grant
                                        }
                                    },
                                )
                            },
                            onClick = {
                                val grant = !allGranted
                                packages?.forEach { pi ->
                                    val uid = pi.applicationInfo?.uid ?: return@forEach
                                    if (grant) {
                                        AuthorizationManager.grant(pi.packageName, uid)
                                    } else {
                                        AuthorizationManager.revoke(pi.packageName, uid)
                                    }
                                    grantedStates[pi.packageName] = grant
                                }
                            }
                        )
                    }
                }

                // 应用列表
                if (packages != null) {
                    items(packages, key = { it.packageName }) { pi ->
                        MiuixAppItem(
                            packageInfo = pi,
                            isGranted = grantedStates[pi.packageName] ?: false,
                            onToggle = { granted ->
                                val uid = pi.applicationInfo?.uid ?: return@MiuixAppItem
                                if (granted) {
                                    AuthorizationManager.grant(pi.packageName, uid)
                                } else {
                                    AuthorizationManager.revoke(pi.packageName, uid)
                                }
                                grantedStates[pi.packageName] = granted
                            },
                        )
                    }
                }

                // 底部间距
                item {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MiuixAppItem(
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
        AppIconCache.getOrLoadBitmap(context, appInfo!!, userId, size)
    }

    if (showAdbLimitedDialog) {
        MiuixAdbLimitedDialog(onDismiss = { showAdbLimitedDialog = false })
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth(),
        onClick = {
            try {
                if (isGranted) {
                    AuthorizationManager.revoke(packageInfo.packageName, uid)
                } else {
                    AuthorizationManager.grant(packageInfo.packageName, uid)
                }
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
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
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
                        if (granted) {
                            AuthorizationManager.grant(packageInfo.packageName, uid)
                        } else {
                            AuthorizationManager.revoke(packageInfo.packageName, uid)
                        }
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
private fun MiuixAdbLimitedDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    WindowDialog(
        show = true,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.app_management_dialog_adb_is_limited_title),
    ) {
        Column {
            Text(
                text = stringResource(
                    R.string.app_management_dialog_adb_is_limited_message,
                    Helps.ADB.get()
                ).toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE).toString(),
                style = MiuixTheme.textStyles.body2,
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                text = stringResource(android.R.string.ok),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
