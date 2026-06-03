package moe.shizuku.manager.starter

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import moe.shizuku.manager.AppConstants.EXTRA
import moe.shizuku.manager.R
import moe.shizuku.manager.adb.AdbKeyException
import moe.shizuku.manager.adb.AdbStarter
import moe.shizuku.manager.app.AppActivity
import moe.shizuku.manager.compose.screens.StarterScreen
import moe.shizuku.manager.compose.theme.ShizukuTheme
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.lifecycle.Resource

internal class NotRootedException : Exception()

class StarterActivity : AppActivity() {

    private val viewModel: ViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ShizukuTheme {
                StarterScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                )
            }
        }
    }

    private var hasStarted = false

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !hasStarted) {
            hasStarted = true
            viewModel.start(
                intent.getBooleanExtra(EXTRA_IS_ROOT, false),
                intent.getIntExtra(EXTRA_PORT, 0),
            )
        }
    }

    companion object {
        const val EXTRA_IS_ROOT = "$EXTRA.IS_ROOT"
        const val EXTRA_PORT = "$EXTRA.PORT"
    }
}

class ViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = getApplication<Application>().applicationContext

    private val sb = StringBuilder()
    private val _output = MutableLiveData<Resource<StringBuilder>>()

    val output = _output as LiveData<Resource<StringBuilder>>

    private val handler = CoroutineExceptionHandler { _, throwable ->
        ShizukuStateMachine.update()
        log(error = throwable)
    }

    private var started = false

    fun start(root: Boolean, port: Int) {
        if (started) return
        started = true

        viewModelScope.launch(handler) {
            if (root) startRoot()
            else AdbStarter.startAdb(appContext, port, { log(it) })
            Starter.waitForBinder({ log(it) })
        }
    }

    private fun log(line: String? = null, error: Throwable? = null) {
        line?.let { sb.appendLine(it) }
        error?.let { sb.appendLine().appendLine(Log.getStackTraceString(it)) }

        if (error == null) _output.postValue(Resource.success(sb))
        else _output.postValue(Resource.error(error, sb))
    }

    private suspend fun startRoot() {
        log("Starting with root…\n")

        withContext(Dispatchers.IO) {
            if (!Shell.getShell().isRoot) {
                Shell.getCachedShell()?.close()
                log("Can't open root shell, try again…")

                if (!Shell.getShell().isRoot) {
                    Shell.getCachedShell()?.close()
                    log("Still not :(")
                    throw NotRootedException()
                }
            }

            ShizukuStateMachine.set(ShizukuStateMachine.State.STARTING)
            suspendCancellableCoroutine { cont ->
                Shell.cmd(Starter.internalCommand).to(object : CallbackList<String?>() {
                    override fun onAddElement(s: String?) {
                        s?.let { log(it) }
                    }
                }).submit {
                    if (it.isSuccess) {
                        cont.resume(Unit)
                    } else {
                        log("\nSend this to developer may help solve the problem.")
                        cont.resumeWithException(Exception("Failed to start with root"))
                    }
                }
            }
        }
    }
}
