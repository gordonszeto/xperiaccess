package xyz.gordonszeto.xperiaccess.util.logcat

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

typealias ResultCallback = (String) -> Unit

private const val tag = "LogcatReader"

class LogcatReader(
    private val callback: ResultCallback,
    private val lifecycle: Lifecycle,
    private val args: List<String> = listOf(),
) : DefaultLifecycleObserver {
    private var logcatProcess: Process? = null
    private var enabled = false

    init {
        lifecycle.addObserver(this)
    }

    fun start() {
        Log.d(tag, "start")
        if (!shouldStart()) {
            return
        }

        enabled = true
        lifecycle.coroutineScope.launch(Dispatchers.IO) {
//            clear()

            if (!shouldStart()) {
                return@launch
            }

            try {
                val cmd = listOf("logcat", "-T", "1") + args
                val pb = ProcessBuilder(cmd)
                logcatProcess = pb.start()

                flow.collect { line -> callback(line) }
            } finally {
                kill()
                if (enabled && shouldStart()) {
                    start()
                }
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stop()
    }

    fun stop() {
        Log.d(tag, "stop")
        enabled = false
        kill()
    }

    private val flow get() = flow {
        val logStream = logcatProcess?.inputStream ?: return@flow
        logStream.use { logStream ->
            logStream.bufferedReader().use { bufReader ->
                while (enabled && logcatProcess?.isAlive == true) {
                    try {
                        val line = bufReader.readLine() ?: break
                        emit(line)
                    } catch (e: IOException) {
                        Log.d(tag, "Process InputStream threw: ${e.message}")
                        break
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

//    private suspend fun clear() = coroutineScope {
//        launch(Dispatchers.IO) {
//            val process = ProcessBuilder("logcat", "-c").start()
//            process.waitFor()
//        }
//    }

    private fun shouldStart(): Boolean {
        Log.d(tag, "Lifecycle state: ${lifecycle.currentState}")
        return lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)
    }

    private fun kill() {
        Log.d(tag, "kill")
        logcatProcess?.destroy()
        logcatProcess = null
    }
}