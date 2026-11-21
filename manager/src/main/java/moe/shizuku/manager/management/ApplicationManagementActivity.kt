package moe.shizuku.manager.management

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import moe.shizuku.manager.app.AppActivity
import moe.shizuku.manager.compose.screens.AppManagementScreen
import moe.shizuku.manager.compose.screens.MiuixAppManagementScreen
import moe.shizuku.manager.compose.components.miuix.Os4Scaffold
import moe.shizuku.manager.compose.theme.LocalIsMiuix
import moe.shizuku.manager.compose.theme.LocalThemeState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import moe.shizuku.manager.R
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.rememberGlassSearchBarAlpha
import top.yukonga.miuix.kmp.glass.glassSearchBarTransition
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.Search
import moe.shizuku.manager.compose.theme.ShizukuAppTheme
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.lifecycle.Status
import java.util.Objects

class ApplicationManagementActivity : AppActivity() {

    private val viewModel: AppsViewModel by viewModels()

    private val stateListener: (ShizukuStateMachine.State) -> Unit = {
        if (ShizukuStateMachine.isDead() && !isFinishing)
            finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ShizukuStateMachine.isRunning()) {
            finish()
            return
        }

        setContent {
            ShizukuAppTheme {
                if (LocalIsMiuix.current) {
                    var searchExpanded by rememberSaveable { mutableStateOf(false) }
                    val searchBarAlpha = rememberGlassSearchBarAlpha(searchExpanded)
                    val blurEnabled = LocalThemeState.current.enableBlur
                    BackHandler(enabled = searchExpanded) { searchExpanded = false }
                    Os4Scaffold(
                        title = stringResource(R.string.home_app_management_title),
                        onBack = { finish() },
                        topBarAlpha = { searchBarAlpha.value },
                        topBarContentModifier = Modifier.glassSearchBarTransition(searchExpanded, blurEnabled = blurEnabled) { 1f },
                        actions = {
                            GlassIconButton(onClick = { searchExpanded = true }) {
                                Icon(MiuixIcons.Glass.Search, stringResource(R.string.search_apps), Modifier.size(28.dp))
                            }
                        },
                    ) { padding, behavior, contentModifier ->
                        MiuixAppManagementScreen(
                            viewModel = viewModel,
                            scrollBehavior = behavior,
                            searchExpanded = searchExpanded,
                            onSearchExpandedChange = { searchExpanded = it },
                            pageContent = { listContent ->
                                Box(
                                    Modifier.fillMaxSize()
                                        .then(contentModifier)
                                        .graphicsLayer { alpha = searchBarAlpha.value }
                                        .then(if (searchExpanded) Modifier.clearAndSetSemantics { } else Modifier),
                                ) { listContent(padding) }
                            },
                        )
                    }
                } else {
                    AppManagementScreen(viewModel = viewModel, onNavigateBack = { finish() })
                }
            }
        }

        ShizukuStateMachine.addListener(stateListener)
    }

    override fun onDestroy() {
        ShizukuStateMachine.removeListener(stateListener)
        super.onDestroy()
    }
}
