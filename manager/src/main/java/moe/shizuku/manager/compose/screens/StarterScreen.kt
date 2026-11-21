package moe.shizuku.manager.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import moe.shizuku.manager.R
import moe.shizuku.manager.adb.AdbKeyException
import moe.shizuku.manager.compose.components.miuix.LocalPageBackdrop
import moe.shizuku.manager.compose.components.miuix.Os4Scaffold
import moe.shizuku.manager.compose.theme.LocalIsMiuix
import moe.shizuku.manager.starter.NotRootedException
import moe.shizuku.manager.starter.Starter
import moe.shizuku.manager.starter.ViewModel
import rikka.lifecycle.Status
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.glass.GlassDialog
import top.yukonga.miuix.kmp.glass.GlassDialogButton
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import java.net.ConnectException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLProtocolException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarterScreen(
    viewModel: ViewModel,
    onNavigateBack: () -> Unit,
) {
    val outputResource by viewModel.output.observeAsState()
    val output = outputResource?.data?.trim()?.toString() ?: ""
    val isError = outputResource?.status == Status.ERROR

    var errorMessageRes by remember { mutableStateOf<Int?>(null) }
    var retainedErrorMessageRes by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(outputResource) {
        val resource = outputResource ?: return@LaunchedEffect
        val trimmedOutput = resource.data?.trim() ?: return@LaunchedEffect

        if (trimmedOutput.endsWith(Starter.serviceStartedMessage)) {
            delay(3000)
            onNavigateBack()
        } else if (resource.status == Status.ERROR) {
            val messageRes = when (resource.error) {
                is AdbKeyException -> R.string.adb_error_key_store
                is NotRootedException -> R.string.start_with_root_failed
                is SocketTimeoutException -> R.string.cannot_connect_port
                is ConnectException -> R.string.cannot_connect_port
                is SSLProtocolException -> R.string.adb_pair_required
                else -> null
            }
            if (messageRes != null) {
                retainedErrorMessageRes = messageRes
                errorMessageRes = messageRes
            }
        }
    }

    if (LocalIsMiuix.current) {
        Os4Scaffold(
            title = stringResource(R.string.starter),
            onBack = onNavigateBack,
            closeAction = true,
        ) { paddingValues, scrollBehavior, contentModifier ->
            GlassDialog(
                show = errorMessageRes != null,
                backdrop = LocalPageBackdrop.current,
                onDismissRequest = { errorMessageRes = null },
                title = stringResource(R.string.starter),
                summary = retainedErrorMessageRes?.let { stringResource(it) },
                onDismissFinished = {
                    if (errorMessageRes == null) retainedErrorMessageRes = null
                },
            ) {
                GlassDialogButton(
                    onClick = { errorMessageRes = null },
                    modifier = Modifier.fillMaxWidth(),
                    role = GlassDialogButtonRole.Primary,
                ) {
                    Text(
                        text = stringResource(android.R.string.ok),
                        style = MiuixTheme.textStyles.button,
                    )
                }
            }
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
            ) {
                Text(
                    text = output.ifEmpty { stringResource(R.string.starting_root_shell) },
                    style = MiuixTheme.textStyles.body2.copy(fontFamily = FontFamily.Monospace),
                    color = if (isError) MiuixTheme.colorScheme.error
                    else MiuixTheme.colorScheme.onSurface,
                )
            }
        }
        return
    }

    if (errorMessageRes != null) {
        val message = stringResource(errorMessageRes!!)
        val okText = stringResource(android.R.string.ok)

        AlertDialog(
            onDismissRequest = { errorMessageRes = null },
            text = { androidx.compose.material3.Text(message) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { errorMessageRes = null }) {
                    androidx.compose.material3.Text(okText)
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { androidx.compose.material3.Text(stringResource(R.string.starter)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
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
        ) {
            androidx.compose.material3.Text(
                text = output.ifEmpty { stringResource(R.string.starting_root_shell) },
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
