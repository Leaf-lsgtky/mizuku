package moe.shizuku.manager.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import moe.shizuku.manager.R
import moe.shizuku.manager.adb.AdbKeyException
import moe.shizuku.manager.starter.NotRootedException
import moe.shizuku.manager.starter.Starter
import moe.shizuku.manager.starter.ViewModel
import rikka.lifecycle.Status
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
    val output = outputResource?.data?.trim() ?: ""
    val isError = outputResource?.status == Status.ERROR

    var errorMessageRes by remember { mutableStateOf<Int?>(null) }

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
                errorMessageRes = messageRes
            }
        }
    }

    if (errorMessageRes != null) {
        AlertDialog(
            onDismissRequest = { errorMessageRes = null },
            text = { Text(stringResource(errorMessageRes!!)) },
            confirmButton = {
                TextButton(onClick = { errorMessageRes = null }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.starter)) },
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
            Text(
                text = output.ifEmpty { stringResource(R.string.starting_root_shell) },
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
