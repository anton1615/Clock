package com.anton.clock.core

import android.app.AlarmManager
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
    private val ACTION_TRIGGER_SOUND = "com.anton.clock.TRIGGER_SOUND"
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val binder = TimerBinder()
    
    val signalRManager = SignalRManager()
    val engine = PomodoroEngine()
    
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var alarmManager: AlarmManager
    private var currentSettings = AppSettings()
    
    private var isBound = false
    private val CHANNEL_ID = "clock_sync_channel_v3" // 更新 ID
    private val NOTIFICATION_ID = 888

    private var soundJob: Job? = null
    private var lastScheduledSeconds: Double = 0.0

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
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
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
                updateNotification()
            }
        }
        
        // 套用初始設定
        engine.updateDurations(currentSettings.workDuration, currentSettings.breakDuration)
        
        // 監聽 SignalR 狀態與引擎狀態
        serviceScope.launch {
            signalRManager.connectionState.collectLatest { state ->
                val isConnected = state == HubConnectionState.CONNECTED
                engine.setSyncStatus(isConnected)
                
                if (isConnected) {
                    Log.d(TAG, "Connected to PC, syncing sound soon...")
                    // 連上時不立即 cancel，等第一筆 state 進來再處理
                } else {
                    Log.d(TAG, "Disconnected from PC, returning to local defaults.")
                    engine.reset(startPaused = true) // 斷線後回到本地預設(25/5分)並暫停
                    scheduleSound() 
                }
                updateNotification()
            }
        }

        serviceScope.launch {
            signalRManager.lastState.collectLatest { state ->
                state?.let { 
                    engine.applyState(it)
                    // 檢查時間同步後的偏差，如果落差超過 2 秒，重新預約音效
                    // 這解決了「連上 PC 後秒數跳變」以及「PC 中途改時間」音效沒重對齊的問題
                    if (Math.abs(it.remainingSeconds - lastScheduledSeconds) > 2.0) {
                        Log.d(TAG, "Sync drift detected (${it.remainingSeconds} vs $lastScheduledSeconds), rescheduling sound.")
                        scheduleSound()
                    }
                }
            }
        }
        
        serviceScope.launch {
            engine.isWorkPhase.collect { 
                scheduleSound()
                updateNotification()
            }
        }

        serviceScope.launch {
            engine.isPaused.collectLatest { 
                scheduleSound()
                updateNotification() 
            }
        }

        serviceScope.launch {
            engine.isSynced.collectLatest { updateNotification() }
        }
        
        // 啟動前台服務
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun getSoundPendingIntent(): PendingIntent {
        val intent = Intent(this, TimerService::class.java).apply {
            action = ACTION_TRIGGER_SOUND
        }
        return PendingIntent.getService(
            this, 1, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun scheduleSound() {
        soundJob?.cancel()
        alarmManager.cancel(getSoundPendingIntent())
        
        val isPaused = engine.isPaused.value
        if (isPaused) {
            Log.d(TAG, "Timer paused, sound scheduling cancelled.")
            lastScheduledSeconds = 0.0
            return
        }

        val remainingSecs = engine.remainingSeconds.value
        val remainingMillis = (remainingSecs * 1000.0).toLong()
        if (remainingMillis <= 0) {
            lastScheduledSeconds = 0.0
            return
        }

        lastScheduledSeconds = remainingSecs
        Log.d(TAG, "Scheduling sound in ${remainingMillis}ms (AlarmManager + Coroutine)")
        
        // 1. AlarmManager 保險 (螢幕關閉、Doze 模式必備)
        val triggerTime = SystemClock.elapsedRealtime() + remainingMillis
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerTime,
                    getSoundPendingIntent()
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerTime,
                    getSoundPendingIntent()
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to set exact alarm (permission missing?), falling back to non-exact.", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerTime,
                getSoundPendingIntent()
            )
        }

        // 2. Coroutine delay (App 活著時的快速反應)
        soundJob = serviceScope.launch {
            delay(remainingMillis)
            // 再次檢查是否仍未暫停
            if (!engine.isPaused.value) {
                Log.d(TAG, "Coroutine sound trigger fired!")
                playDefaultNotificationSound()
            }
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        currentSettings = newSettings
        engine.updateDurations(newSettings.workDuration, newSettings.breakDuration)
        scheduleSound() // 更新時長後重新預約
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
            if (ringtone != null && !ringtone.isPlaying) {
                ringtone.play()
            }
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
        
        val phaseName = if (isWork) "WORKING" else "BREAKING"
        val statusText = buildString {
            if (isPaused) append("PAUSED") else append("RUNNING")
            if (isSynced) append(" • SYNCED")
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
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)

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
        if (intent?.action == ACTION_TRIGGER_SOUND) {
            Log.d(TAG, "Sound triggered via AlarmManager action")
            playDefaultNotificationSound()
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "Task removed (app swiped away). Stopping service.")
        stopSelf() // 徹底停止服務並移除通知
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
        soundJob?.cancel() // 明確取消音效預約
        alarmManager.cancel(getSoundPendingIntent())
        serviceScope.cancel()
        signalRManager.disconnect()
    }
}