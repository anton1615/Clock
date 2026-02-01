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
    private var lastScheduledTargetEnd: Long = 0L
    private var lastPlayedTargetTime: Long = 0L
    private var lastObservedPhase: Boolean? = null

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
                    // 修正：增加門檻至 2 秒，避免頻繁更新造成系統節流
                    val drift = if (it.isPaused) 0L else Math.abs(it.targetEndTimeUnix - lastScheduledTargetEnd)
                    if (drift > 2000) { 
                        Log.d(TAG, "Sync target drift detected ($drift ms), rescheduling sound.")
                        scheduleSound()
                    }
                }
            }
        }
        
        serviceScope.launch {
            engine.isWorkPhase.collect { isWork ->
                if (lastObservedPhase != null && lastObservedPhase != isWork) {
                    // 階段切換了！播放「剛結束」階段的音效（對應上一筆預約時間）
                    // 此處對應 Sync 模式下 PC 切換階段，或手動 Skip 的音效觸發
                    playSound(lastScheduledTargetEnd, "PhaseTransition")
                }
                lastObservedPhase = isWork
                lastScheduledTargetEnd = 0L // 重置以確保重新預約
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

    private fun getSoundPendingIntent(targetTime: Long): PendingIntent {
        val intent = Intent(this, TimerService::class.java).apply {
            action = ACTION_TRIGGER_SOUND
            putExtra("TARGET_TIME", targetTime)
        }
        return PendingIntent.getService(
            this, 1, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun scheduleSound() {
        val targetEndTime = engine.getTargetEndTimeUnix()
        
        // 如果目標跟上次預約的一樣，且已經有 Job 或 Alarm 在跑，就不用動
        if (targetEndTime > 0 && targetEndTime == lastScheduledTargetEnd && (soundJob?.isActive == true)) {
            return
        }

        soundJob?.cancel()
        alarmManager.cancel(getSoundPendingIntent(lastScheduledTargetEnd))
        
        val isPaused = engine.isPaused.value
        if (isPaused || targetEndTime <= 0) {
            Log.d(TAG, "Timer paused or no target, sound scheduling cancelled.")
            lastScheduledTargetEnd = 0L
            return
        }

        val remainingSecs = engine.remainingSeconds.value
        val remainingMillis = (remainingSecs * 1000.0).toLong()
        if (remainingMillis < 100) {
            lastScheduledTargetEnd = 0L
            return
        }

        lastScheduledTargetEnd = targetEndTime
        Log.d(TAG, "Scheduling sound for target $targetEndTime in ${remainingMillis}ms")
        
        // 1. AlarmManager 保險 (螢幕關閉、Doze 模式必備)
        val triggerTime = SystemClock.elapsedRealtime() + remainingMillis
        val pi = getSoundPendingIntent(targetEndTime)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pi)
            } else {
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pi)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pi)
        }

        // 2. Coroutine delay (App 活著時的快速反應)
        soundJob = serviceScope.launch {
            delay(remainingMillis)
            if (!engine.isPaused.value) {
                Log.d(TAG, "Coroutine sound trigger fired for $targetEndTime")
                playSound(targetEndTime, "Coroutine")
                
                // 如果是在 Local 模式 (未連線)，由 Coroutine 負責觸發階段切換
                if (!engine.isSynced.value) {
                    Log.d(TAG, "Local mode: Coroutine triggered phase toggle")
                    engine.localTogglePhase()
                }
            }
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        currentSettings = newSettings
        engine.updateDurations(newSettings.workDuration, newSettings.breakDuration)
        scheduleSound() // 更新時長後重新預約
        updateNotification()
    }

    private fun playSound(targetTime: Long, source: String) {
        // 去重機制：如果該目標時間已經在 2 秒內播過了，就跳過
        if (targetTime > 0 && Math.abs(targetTime - lastPlayedTargetTime) < 2000) {
            Log.d(TAG, "Sound already played for target $targetTime via $source, skipping.")
            return
        }
        lastPlayedTargetTime = targetTime
        Log.d(TAG, "Playing sound for target $targetTime (source: $source)")
        playDefaultNotificationSound()
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
            if (ringtone != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ringtone.audioAttributes = android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                if (!ringtone.isPlaying) {
                    ringtone.play()
                }
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
        val remainingSecs = Math.max(0.0, engine.remainingSeconds.value)
        if (!isPaused && remainingSecs > 0.5) { 
            val remainingMillis = (remainingSecs * 1000.0).toLong()
            val targetTime = System.currentTimeMillis() + remainingMillis 
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(true)
            builder.setWhen(targetTime)
        } else {
            builder.setUsesChronometer(false)
            val mins = (remainingSecs / 60).toInt()
            val secs = (remainingSecs % 60).toInt()
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
            val targetTime = intent.getLongExtra("TARGET_TIME", 0L)
            Log.d(TAG, "Sound triggered via AlarmManager for target $targetTime")
            playSound(targetTime, "AlarmManager")
            
            // 如果是在 Local 模式 (未連線)，AlarmManager 到期代表階段結束，必須主動切換
            if (!engine.isSynced.value && !engine.isPaused.value) {
                Log.d(TAG, "Local mode: Alarm triggered phase toggle")
                engine.localTogglePhase()
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "Task removed (app swiped away). Keeping service running in background.")
        // 不再呼叫 stopSelf()，確保背景同步與鬧鐘繼續運作
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
        soundJob?.cancel() // 明確取消音效預約
        alarmManager.cancel(getSoundPendingIntent(lastScheduledTargetEnd))
        serviceScope.cancel()
        signalRManager.disconnect()
    }
}