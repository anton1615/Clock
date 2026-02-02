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
import android.media.SoundPool
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
    
    private val CHANNEL_ID = "clock_sync_channel_v7"
    private val NOTIFICATION_ID = 888
    private val ALARM_REQUEST_CODE = 999 

    private var lastScheduledTargetEnd: Long = 0L
    private var lastPlayedTargetTime: Long = 0L
    private var lastObservedPhase: Boolean? = null
    
    private var soundPool: SoundPool? = null
    private var soundId: Int = -1
    private var updateJob: Job? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "[SCREEN] OFF")
                    updateNotification() 
                    stopUpdateLoop()
                }
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(TAG, "[SCREEN] ON")
                    engine.refreshRemainingSeconds()
                    updateNotification()
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
        
        initSoundPool()
        createNotificationChannel()
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)

        engine.updateDurations(currentSettings.workDuration, currentSettings.breakDuration)
        
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
                    if (Math.abs(it.targetEndTimeUnix - lastScheduledTargetEnd) > 2000) {
                        rescheduleAlarm()
                    }
                }
            }
        }
        
        serviceScope.launch {
            engine.isWorkPhase.collect { isWork ->
                if (lastObservedPhase != null && lastObservedPhase != isWork) {
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

        startForeground(NOTIFICATION_ID, createNotification())
        startUpdateLoop()
    }

    private fun initSoundPool() {
        val attr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(1).setAudioAttributes(attr).build()
        loadCurrentSound()
    }

    private fun loadCurrentSound() {
        val soundUriStr = currentSettings.soundUri
        
        // Unload previous sound to release memory and ensure update
        if (soundId != -1) {
            soundPool?.unload(soundId)
            soundId = -1
        }

        if (soundUriStr == "silent" || soundUriStr == null) return
        
        try {
            val uri = if (soundUriStr == "default") RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                      else Uri.parse(soundUriStr)
            contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                soundId = soundPool?.load(afd, 1) ?: -1
            }
            Log.d(TAG, "[SOUND] Loaded sound: $soundUriStr (ID: $soundId)")
        } catch (e: Exception) { Log.e(TAG, "SoundPool load failed", e) }
    }

    private fun startUpdateLoop() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive) {
                engine.refreshRemainingSeconds()
                updateNotification()
                // 前台/亮屏保險：即便鬧鐘遲到，只要還在跑就補位
                if (engine.remainingSeconds.value <= 0.0 && !engine.isPaused.value && !engine.isSynced.value) {
                    Log.d(TAG, "[DIAG] Local loop forced transition")
                    handleTransition(engine.getTargetEndTimeUnix())
                }
                delay(1000)
            }
        }
    }

    private fun stopUpdateLoop() {
        updateJob?.cancel()
        updateJob = null
    }

    private fun rescheduleAlarm() {
        val targetEndTime = engine.getTargetEndTimeUnix()
        alarmManager.cancel(getSoundPendingIntent(0))

        val isPaused = engine.isPaused.value
        if (isPaused || targetEndTime <= 0) {
            lastScheduledTargetEnd = 0L
            return
        }

        val now = System.currentTimeMillis()
        val remainingMillis = targetEndTime - now
        if (remainingMillis < 100) return

        lastScheduledTargetEnd = targetEndTime
        
        // 核心修正：使用 setAlarmClock 這是 Android 最高權威的鬧鐘預約，絕對不會被 Pixel 延遲
        val showIntent = Intent(this, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(this, 0, showIntent, PendingIntent.FLAG_IMMUTABLE)
        
        val pi = getSoundPendingIntent(targetEndTime)
        
        Log.d(TAG, "[ALARM] Scheduling AlarmClock for $targetEndTime (in ${remainingMillis}ms)")
        
        try {
            // 優先檢查權限 (Android 12+)
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else true

            if (hasPermission) {
                val alarmInfo = AlarmManager.AlarmClockInfo(targetEndTime, showPendingIntent)
                alarmManager.setAlarmClock(alarmInfo, pi)
            } else {
                Log.w(TAG, "[ALARM] Exact permission missing, using non-exact fallback")
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetEndTime, pi)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "[ALARM] SecurityException while scheduling, using safe fallback", e)
            // 這是最終保險：setAndAllowWhileIdle 不需要任何權限
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetEndTime, pi)
        } catch (e: Exception) {
            Log.e(TAG, "[ALARM] Unexpected error during scheduling", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetEndTime, pi)
        }
    }

    private fun getSoundPendingIntent(targetTime: Long): PendingIntent {
        val intent = Intent(this, TimerService::class.java).apply {
            action = ACTION_TRIGGER_SOUND
            putExtra("TARGET_TIME", targetTime)
        }
        return PendingIntent.getService(this, ALARM_REQUEST_CODE, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun handleTransition(targetTime: Long) {
        if (!engine.isSynced.value && !engine.isPaused.value) {
            engine.localTogglePhase()
        }

        serviceScope.launch(Dispatchers.Default) {
            if (targetTime > 0 && Math.abs(targetTime - lastPlayedTargetTime) > 2000) {
                lastPlayedTargetTime = targetTime
                Log.d(TAG, "[SOUND] Triggering sound for target $targetTime")
                wakeScreen(3000)
                if (soundId != -1) soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
            }
            withContext(Dispatchers.Main) { updateNotification() }
        }
    }

    private fun wakeScreen(durationMs: Long) {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "Clock:WakeUp")
            wl.acquire(durationMs)
            Log.d(TAG, "[SCREEN] Waking up")
        } catch (e: Exception) {}
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val isWork = engine.isWorkPhase.value
        val isPaused = engine.isPaused.value
        val isSynced = engine.isSynced.value
        val remainingSecs = Math.max(0.0, engine.remainingSeconds.value)
        val timeStr = String.format("%02d:%02d", (remainingSecs / 60).toInt(), (remainingSecs % 60).toInt())
        val statusText = if (isPaused) "PAUSED" else if (isSynced) "SYNCED" else "RUNNING"
        val baseColorHex = if (isWork) currentSettings.workColor else currentSettings.breakColor
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle(if (isWork) "WORKING" else "BREAKING")
            .setContentText("$statusText • $timeStr")
            .setColor(darkenColor(android.graphics.Color.parseColor(baseColorHex), 0.3f))
            .setColorized(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_TRIGGER_SOUND) {
            val targetTime = intent.getLongExtra("TARGET_TIME", 0L)
            Log.d(TAG, "[ALARM] TRIGGERED at ${System.currentTimeMillis()} for target $targetTime")
            handleTransition(targetTime)
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "[SERVICE] App swiped away, stopping service and notification")
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
        updateJob?.cancel()
        serviceScope.cancel()
        soundPool?.release()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(CHANNEL_ID, "Clock Precise Service", NotificationManager.IMPORTANCE_DEFAULT)
            channel.enableVibration(false)
            channel.setSound(null, null)
            nm.createNotificationChannel(channel)
        }
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val a = android.graphics.Color.alpha(color)
        val r = (android.graphics.Color.red(color) * factor).toInt()
        val g = (android.graphics.Color.green(color) * factor).toInt()
        val b = (android.graphics.Color.blue(color) * factor).toInt()
        return android.graphics.Color.argb(a, r, g, b)
    }

    fun updateSettings(newSettings: AppSettings) {
        currentSettings = newSettings
        engine.updateDurations(newSettings.workDuration, newSettings.breakDuration)
        loadCurrentSound()
        rescheduleAlarm()
        updateNotification()
    }

    fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }
}