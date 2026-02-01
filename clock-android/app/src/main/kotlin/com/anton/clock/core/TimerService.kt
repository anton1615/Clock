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
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
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
    
    private val CHANNEL_ID = "clock_sync_channel_v4"
    private val NOTIFICATION_ID = 888

    private var lastScheduledTargetEnd: Long = 0L
    private var lastPlayedTargetTime: Long = 0L
    private var lastObservedPhase: Boolean? = null
    
    private var mediaPlayer: MediaPlayer? = null
    private var updateJob: Job? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "Screen OFF - Stopping background updates")
                    stopUpdateLoop()
                }
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(TAG, "Screen ON - Resuming background updates")
                    startUpdateLoop()
                }
            }
        }
    }

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        currentSettings = settingsRepository.getSettings()
        
        createNotificationChannel()
        
        // 註冊螢幕廣播
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)

        // 初始化引擎設定
        engine.updateDurations(currentSettings.workDuration, currentSettings.breakDuration)
        
        // 監聽 SignalR 與狀態同步
        serviceScope.launch {
            signalRManager.connectionState.collectLatest { state ->
                val isConnected = state == HubConnectionState.CONNECTED
                engine.setSyncStatus(isConnected)
                if (!isConnected) {
                    engine.reset(startPaused = true)
                    rescheduleAlarm()
                }
                updateNotification()
            }
        }

        serviceScope.launch {
            signalRManager.lastState.collectLatest { state ->
                state?.let { 
                    engine.applyState(it)
                    // 如果目標時間變動超過 2 秒，重新預約
                    if (Math.abs(it.targetEndTimeUnix - lastScheduledTargetEnd) > 2000) {
                        rescheduleAlarm()
                    }
                }
            }
        }
        
        serviceScope.launch {
            engine.isWorkPhase.collect { isWork ->
                if (lastObservedPhase != null && lastObservedPhase != isWork) {
                    // 自然結束或 PC 同步導致的切換
                    if (engine.remainingSeconds.value < 2.0) {
                        handleTransition(engine.getTargetEndTimeUnix())
                    }
                }
                lastObservedPhase = isWork
                rescheduleAlarm()
                updateNotification()
            }
        }

        serviceScope.launch {
            engine.isPaused.collectLatest { 
                rescheduleAlarm()
                updateNotification() 
            }
        }

        // 啟動更新循環 (如果螢幕開著)
        startUpdateLoop()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun startUpdateLoop() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive) {
                engine.refreshRemainingSeconds()
                updateNotification()
                delay(1000) // 背景/通知欄更新頻率 1s
            }
        }
    }

    private fun stopUpdateLoop() {
        updateJob?.cancel()
        updateJob = null
    }

    private fun rescheduleAlarm() {
        val targetEndTime = engine.getTargetEndTimeUnix()
        
        // 取消舊鬧鐘
        if (lastScheduledTargetEnd > 0) {
            val oldPi = getSoundPendingIntent(lastScheduledTargetEnd)
            alarmManager.cancel(oldPi)
        }

        val isPaused = engine.isPaused.value
        if (isPaused || targetEndTime <= 0) {
            lastScheduledTargetEnd = 0L
            return
        }

        val now = System.currentTimeMillis()
        val remainingMillis = targetEndTime - now
        if (remainingMillis < 100) return

        lastScheduledTargetEnd = targetEndTime
        val triggerTime = SystemClock.elapsedRealtime() + remainingMillis
        val pi = getSoundPendingIntent(targetEndTime)
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pi)
            } else {
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pi)
            }
        } catch (e: Exception) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pi)
        }
    }

    private fun getSoundPendingIntent(targetTime: Long): PendingIntent {
        val intent = Intent(this, TimerService::class.java).apply {
            action = ACTION_TRIGGER_SOUND
            putExtra("TARGET_TIME", targetTime)
        }
        return PendingIntent.getService(
            this, targetTime.hashCode(), intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * 核心轉場處理：播放音效、喚醒、切換
     */
    private fun handleTransition(targetTime: Long) {
        // 1. 【立即執行】本地轉場優先，讓 UI 數字與通知列立刻動起來，不等待音訊系統
        if (!engine.isSynced.value && !engine.isPaused.value) {
            Log.d(TAG, "Transitioning phase immediately for target $targetTime")
            engine.localTogglePhase()
            updateNotification()
        }

        // 2. 【非同步執行】去重播放與喚醒，避免阻塞主執行緒導致 UI 卡死
        serviceScope.launch(Dispatchers.Default) {
            if (targetTime > 0 && Math.abs(targetTime - lastPlayedTargetTime) > 2000) {
                lastPlayedTargetTime = targetTime
                wakeScreen(3000)
                playSoundViaMediaPlayer()
            }
        }
    }

    private fun playSoundViaMediaPlayer() {
        val soundUriStr = currentSettings.soundUri
        if (soundUriStr == "silent" || soundUriStr == null) return
        
        try {
            val uri = if (soundUriStr == "default") {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            } else {
                Uri.parse(soundUriStr)
            }

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION) // 改為 SONIFICATION 代表提示音
                        .build()
                )
                // 關鍵：使用非同步準備，並在準備好後立即播放，防止阻塞主執行緒
                setOnPreparedListener { 
                    it.start()
                    Log.d(TAG, "MediaPlayer started playing asynchronously")
                }
                setOnCompletionListener { 
                    it.release() 
                    if (mediaPlayer == it) mediaPlayer = null
                }
                prepareAsync() 
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaPlayer initialization failed", e)
        }
    }

    private fun wakeScreen(durationMs: Long) {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "Clock:WakeUp"
            )
            wakeLock.acquire(durationMs)
        } catch (e: Exception) {}
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val isWork = engine.isWorkPhase.value
        val isPaused = engine.isPaused.value
        val isSynced = engine.isSynced.value
        val remainingSecs = Math.max(0.0, engine.remainingSeconds.value)
        
        val phaseName = if (isWork) "WORKING" else "BREAKING"
        val statusText = if (isPaused) "PAUSED" else if (isSynced) "SYNCED" else "RUNNING"
        
        val timeStr = String.format("%02d:%02d", (remainingSecs / 60).toInt(), (remainingSecs % 60).toInt())
        
        val baseColorHex = if (isWork) currentSettings.workColor else currentSettings.breakColor
        val darkenedColor = darkenColor(android.graphics.Color.parseColor(baseColorHex), 0.3f)
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle(phaseName)
            .setContentText("$statusText • $timeStr")
            .setColor(darkenedColor)
            .setColorized(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_TRIGGER_SOUND) {
            val targetTime = intent.getLongExtra("TARGET_TIME", 0L)
            handleTransition(targetTime)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
        updateJob?.cancel()
        serviceScope.cancel()
        mediaPlayer?.release()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Clock Sync", NotificationManager.IMPORTANCE_LOW)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        currentSettings = newSettings
        engine.updateDurations(newSettings.workDuration, newSettings.breakDuration)
        rescheduleAlarm()
        updateNotification()
    }

    fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }
}