package moe.shizuku.manager.compose.screens

import android.content.pm.PackageInfo
import android.os.Process
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun MiuixAppManagementScreen(
    viewModel: moe.shizuku.manager.management.AppsViewModel,
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

    if (packages.isNullOrEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.home_app_management_empty),
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item {
                val allGranted = packages.all { pi ->
                    val uid = pi.applicationInfo?.uid ?: return@all true
                    grantedStates[pi.packageName] ?: AuthorizationManager.granted(pi.packageName, uid)
                }
                MiuixToggleAllRow(
                    allGranted = allGranted,
                    onToggleAll = { grant ->
                        packages.forEach { pi ->
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
            }

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
    }
}

@Composable
private fun MiuixToggleAllRow(allGranted: Boolean, onToggleAll: (Boolean) -> Unit) {
    BasicComponent(
        title = stringResource(R.string.app_management_toggle_all),
        endActions = {
            Switch(
                checked = allGranted,
                onCheckedChange = { onToggleAll(it) },
            )
        },
        onClick = { onToggleAll(!allGranted) }
    )
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

    BasicComponent(
        title = label.toString(),
        summary = packageInfo.packageName,
        startAction = {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
        },
        endActions = {
            Column(horizontalAlignment = Alignment.End) {
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
                if (requiresRoot) {
                    Text(
                        text = stringResource(R.string.app_management_item_summary_requires_root),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
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
        }
    )
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
            AndroidView(
                factory = { ctx ->
                    android.widget.TextView(ctx).apply {
                        movementMethod = android.text.method.LinkMovementMethod.getInstance()
                        text = ctx.getString(
                            R.string.app_management_dialog_adb_is_limited_message,
                            Helps.ADB.get()
                        ).toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
                    }
                },
                update = { tv ->
                    tv.text = tv.context.getString(
                        R.string.app_management_dialog_adb_is_limited_message,
                        Helps.ADB.get()
                    ).toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
            top.yukonga.miuix.kmp.basic.TextButton(
                text = stringResource(android.R.string.ok),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
