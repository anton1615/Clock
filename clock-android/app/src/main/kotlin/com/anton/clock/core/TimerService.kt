package com.anton.clock.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.RingtoneManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.anton.clock.MainActivity
import com.anton.clock.R
import com.microsoft.signalr.HubConnectionState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

import com.anton.clock.models.AppSettings

class TimerService : Service() {
    private val TAG = "TimerService"
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val binder = TimerBinder()
    
    val signalRManager = SignalRManager()
    val engine = PomodoroEngine()
    
    private lateinit var settingsRepository: SettingsRepository
    private var currentSettings = AppSettings()
    
    private var isBound = false
    private val CHANNEL_ID = "clock_sync_channel_v3" // 更新 ID
    private val NOTIFICATION_ID = 888

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "Screen OFF detected")
                    engine.setScreenOn(false)
                }
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(TAG, "Screen ON detected")
                    engine.setScreenOn(true)
                    updateNotification() // 螢幕開啟時立即刷新通知
                }
            }
        }
    }

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent?): IBinder {
        isBound = true
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isBound = false
        return true
    }

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        currentSettings = settingsRepository.getSettings()
        
        createNotificationChannel()
        engine.start()
        
        // 註冊螢幕廣播
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)

        // 監聽 App 生命週期狀態並傳給引擎
        serviceScope.launch {
            AppLifecycleObserver.getInstance().isForeground.collectLatest { isForeground ->
                engine.setForeground(isForeground)
            }
        }
        
        // 套用初始設定
        engine.updateDurations(currentSettings.workDuration, currentSettings.breakDuration)
        
        // 監聽 SignalR 狀態與引擎狀態
        serviceScope.launch {
            signalRManager.connectionState.collectLatest { state ->
                engine.setSyncStatus(state == HubConnectionState.CONNECTED)
                updateNotification()
            }
        }

        serviceScope.launch {
            signalRManager.lastState.collectLatest { state ->
                state?.let { engine.applyState(it) }
            }
        }
        
        var lastPhase = engine.isWorkPhase.value
        serviceScope.launch {
            engine.isWorkPhase.collect { isWork ->
                if (isWork != lastPhase) {
                    updateNotification()
                    lastPhase = isWork
                }
            }
        }

        // 優化音效邏輯：監聽時間變動，當時間從 >0 變為 0 附近時播放
        var wasNearZero = false
        serviceScope.launch {
            engine.remainingSeconds.collect { remaining ->
                if (remaining <= 0.1 && !wasNearZero && !engine.isPaused.value) {
                    playDefaultNotificationSound()
                    wasNearZero = true
                } else if (remaining > 1.0) {
                    wasNearZero = false
                }
            }
        }
        
        serviceScope.launch {
            engine.isPaused.collectLatest { updateNotification() }
        }

        serviceScope.launch {
            engine.isSynced.collectLatest { updateNotification() }
        }
        
        // 啟動前台服務
        startForeground(NOTIFICATION_ID, createNotification())
    }

    fun updateSettings(newSettings: AppSettings) {
        currentSettings = newSettings
        engine.updateDurations(newSettings.workDuration, newSettings.breakDuration)
        updateNotification()
    }

    private fun playDefaultNotificationSound() {
        val soundUriStr = currentSettings.soundUri
        if (soundUriStr == "silent" || soundUriStr == null) return
        
        try {
            val uri = if (soundUriStr == "default") {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            } else {
                Uri.parse(soundUriStr)
            }
            val ringtone = RingtoneManager.getRingtone(applicationContext, uri)
            ringtone.play()
        } catch (e: Exception) {
            Log.e("TimerService", "Failed to play notification sound", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Clock Synchronization"
            val descriptionText = "Displays real-time countdown from PC"
            val importance = NotificationManager.IMPORTANCE_DEFAULT // 改為 DEFAULT 以移除頂部橫幅
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val isWork = engine.isWorkPhase.value
        val isPaused = engine.isPaused.value
        val isSynced = engine.isSynced.value
        val isScreenOn = engine.isScreenOn.value
        
        val phaseName = if (isWork) "WORKING" else "BREAKING"
        val statusText = buildString {
            if (isPaused) append("PAUSED") else append("RUNNING")
            if (isSynced) append(" • SYNCED")
            if (!isScreenOn) append(" • Lpm") // Low Power Mode indicator
        }
        
        val baseColorHex = if (isWork) currentSettings.workColor else currentSettings.breakColor
        val baseColor = android.graphics.Color.parseColor(baseColorHex)
        val darkenedColor = darkenColor(baseColor, 0.3f) 
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle(phaseName)
            .setContentText(statusText)
            .setColor(darkenedColor)
            .setColorized(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // 改為 DEFAULT
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS) // 改為 PROGRESS

        // 倒數計時邏輯 (使用 Wall Clock Timebase)
        if (!isPaused) {
            val remainingMillis = (engine.remainingSeconds.value * 1000.0).toLong()
            val targetTime = System.currentTimeMillis() + remainingMillis 
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(true)
            builder.setWhen(targetTime)
        } else {
            builder.setUsesChronometer(false)
            val mins = (engine.remainingSeconds.value / 60).toInt()
            val secs = (engine.remainingSeconds.value % 60).toInt()
            builder.setContentText("$statusText • ${String.format("%02d:%02d", mins, secs)}")
        }

        return builder.build()
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val a = android.graphics.Color.alpha(color)
        val r = Math.round(android.graphics.Color.red(color) * factor)
        val g = Math.round(android.graphics.Color.green(color) * factor)
        val b = Math.round(android.graphics.Color.blue(color) * factor)
        return android.graphics.Color.argb(a,
            Math.min(r, 255),
            Math.min(g, 255),
            Math.min(b, 255))
    }

    fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
        serviceScope.cancel()
        signalRManager.disconnect()
    }
}
