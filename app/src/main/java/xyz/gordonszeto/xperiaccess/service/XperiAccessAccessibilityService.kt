package xyz.gordonszeto.xperiaccess.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.gordonszeto.xperiaccess.util.logcat.LogcatReader

class XperiAccessAccessibilityService : AccessibilityService(), LifecycleOwner {
    private val dispatcher = ServiceLifecycleDispatcher(this)

    private lateinit var powerManager: PowerManager

    private val logcat = LogcatReader(
        ::logcatCallback,
        lifecycle,
        args = listOf("-b", "main", "-s", "KeyButtonView:I"),
    )

    private val screenEventFilter = IntentFilter()
    init {
        screenEventFilter.addAction(Intent.ACTION_SCREEN_OFF)
        screenEventFilter.addAction(Intent.ACTION_SCREEN_ON)
    }

    private val screenEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action?.equals(Intent.ACTION_SCREEN_OFF) == true) {
                Log.d(TAG, "Screen off - Stopping logcat")
                logcat.stop()
            } else if (intent?.action?.equals(Intent.ACTION_SCREEN_ON) == true) {
                Log.d(TAG, "Screen on - Starting logcat")
                logcat.start()
            }
        }
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        dispatcher.onServicePreSuperOnBind()
        Log.d(TAG, "onServiceConnected")
        super.onServiceConnected()

        powerManager = getSystemService()!!
        if (powerManager.isInteractive) {
            Log.d(TAG, "Service connected - Starting logcat")
            logcat.start()
        }

        registerReceiver(screenEventReceiver, screenEventFilter)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val eventType = event?.eventType
        val packageName = event?.packageName
        val className = event?.className
        Log.i(TAG, "Event received (Type: $eventType, Package: $packageName, Class: $className)")
    }

    private var lockScreenJob: Job? = null
    private fun onBackPressed() {
        if (lockScreenJob?.isActive == true) {
            return
        }

        lockScreenJob = lifecycle.coroutineScope.launch {
            delay(LOCK_DELAY_MS)
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
    }

    private fun onBackReleased() {
        lockScreenJob?.cancel()
    }

    override fun getLifecycle() = dispatcher.lifecycle

    private fun logcatCallback(line: String) {
        if (line.contains("Back button event: ACTION_DOWN")) {
            Log.i(TAG,"Back button pressed")
            onBackPressed()
            return
        }

        if (line.contains("Back button event: ACTION_UP")) {
            Log.i(TAG,"Back button released")
            onBackReleased()
            return
        }
    }

    override fun onCreate() {
        dispatcher.onServicePreSuperOnCreate()
        Log.d(TAG, "onCreate")
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        dispatcher.onServicePreSuperOnStart()
        Log.d(TAG, "onStartCommand")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        dispatcher.onServicePreSuperOnDestroy()
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }

    companion object {
        private const val TAG = "XperiAccessAccessibilityService"
        private const val LOCK_DELAY_MS: Long = 500
    }
}