package com.anton.clock.core

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
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

class TimerService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val binder = TimerBinder()
    
    val signalRManager = SignalRManager()
    val engine = PomodoroEngine()
    
    private var isBound = false
    private val CHANNEL_ID = "clock_sync_channel"
    private val NOTIFICATION_ID = 888

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
        createNotificationChannel()
        engine.start()
        
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
                    val remaining = engine.remainingSeconds.value
                    // 如果剩餘時間接近 0 (或是剛重置後的新時間)，且是自動跳轉，才播放音效
                    // 這裡邏輯改為：如果剩餘時間大於某個門檻(代表是手動切換後的新時間)，且跳轉前時間很小，才播放
                    // 實際上最準確的是檢查切換瞬間的 remainingSeconds 是否接近 0
                    if (engine.remainingSeconds.value > 10.0) { // 代表已經重置為新階段的時間了
                         // 這裡無法得知切換前的精確時間，改用另一種邏輯
                    }
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
        
        // 啟動前台服務
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun playDefaultNotificationSound() {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, notificationUri)
            ringtone.play()
        } catch (e: Exception) {
            Log.e("TimerService", "Failed to play notification sound", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Clock Synchronization"
            val descriptionText = "Displays real-time countdown from PC"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
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
        val phaseName = if (isWork) "WORKING" else "BREAKING"
        val accentColor = if (isWork) 0xFFFF8C00.toInt() else 0xFF32CD32.toInt()
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle(if (isPaused) "$phaseName (PAUSED)" else phaseName)
            .setContentText("Syncing with PC...")
            .setColor(accentColor)
            .setColorized(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        // 倒數計時邏輯 (Chronometer)
        if (!isPaused) {
            val remainingMillis = (engine.remainingSeconds.value * 1000.0).toLong()
            val targetTime = SystemClock.elapsedRealtime() + remainingMillis
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(true)
            builder.setWhen(targetTime)
        } else {
            builder.setUsesChronometer(false)
        }

        return builder.build()
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
        serviceScope.cancel()
        signalRManager.disconnect()
    }
}
