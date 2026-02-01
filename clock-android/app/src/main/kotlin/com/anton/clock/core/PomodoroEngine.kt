package com.anton.clock.core

import com.anton.clock.models.EngineState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

class PomodoroEngine(
    var workDurationMinutes: Int = 25,
    var breakDurationMinutes: Int = 5
) {
    private val _remainingSeconds = MutableStateFlow(workDurationMinutes * 60.0)
    val remainingSeconds = _remainingSeconds.asStateFlow()

    private val _isWorkPhase = MutableStateFlow(true)
    val isWorkPhase = _isWorkPhase.asStateFlow()

    private val _isPaused = MutableStateFlow(true) 
    val isPaused = _isPaused.asStateFlow()

    private val _isSynced = MutableStateFlow(false)
    val isSynced = _isSynced.asStateFlow()

    private val _isForeground = MutableStateFlow(true)
    val isForeground = _isForeground.asStateFlow()

    private val _isScreenOn = MutableStateFlow(true)
    val isScreenOn = _isScreenOn.asStateFlow()

    private var nextWorkMins: Int = workDurationMinutes
    private var nextBreakMins: Int = breakDurationMinutes

    fun setForeground(foreground: Boolean) { _isForeground.value = foreground }
    fun setScreenOn(on: Boolean) { _isScreenOn.value = on }

    fun updateDurations(work: Int, breakM: Int) {
        nextWorkMins = work
        nextBreakMins = breakM
    }

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    
    private var clockOffsetRolling: Double? = null
    private val smoothingFactor = 0.15 // 降低一點權重，更穩定
    private var lastState: EngineState? = null

    fun start() {
        timerJob?.cancel()
        timerJob = scope.launch {
            var lastTime = System.currentTimeMillis()
            while (isActive) {
                // 動態調整 Delay
                val currentDelay = when {
                    _isPaused.value -> 500L
                    !_isScreenOn.value -> 2000L // 螢幕關閉時，每 2 秒才檢查一次（極致省電）
                    !_isForeground.value -> 1000L // 後台時，每 1 秒更新一次（配合通知欄）
                    else -> 50L // 前台時，50ms（流暢動畫）
                }
                
                delay(currentDelay)
                
                val now = System.currentTimeMillis()
                
                // 如果螢幕關閉且不是暫停狀態，我們不主動遞減時間，
                // 因為恢復時會透過 TargetTime 重新計算。
                // 這樣可以完全移除這段時間內的 CPU 運算。
                
                if (_isSynced.value && lastState != null) {
                    val state = lastState!!
                    if (!state.isPaused && state.targetEndTimeUnix > 0) {
                        val adjustedNow = now.toDouble() + (clockOffsetRolling ?: 0.0)
                        val diff = state.targetEndTimeUnix.toDouble() - adjustedNow
                        _remainingSeconds.value = if (diff > 0) (diff / 1000.0) else 0.0
                    } else {
                        _remainingSeconds.value = state.remainingSeconds
                    }
                } else if (!_isPaused.value) {
                    val delta = (now - lastTime) / 1000.0
                    if (_remainingSeconds.value > 0) {
                        _remainingSeconds.value -= delta
                    }
                }
                lastTime = now
            }
        }
    }

    fun setSyncStatus(synced: Boolean) {
        _isSynced.value = synced
        if (!synced) {
            clockOffsetRolling = null
            lastState = null
        }
    }

    fun applyState(state: EngineState) {
        lastState = state
        
        // 1. 先計算時間偏差與補償
        if (!state.isPaused && state.targetEndTimeUnix > 0) {
            val now = System.currentTimeMillis()
            // 將補償值提高到 150ms 以趕上電腦
            val currentInstantOffset = (state.targetEndTimeUnix - (state.remainingSeconds * 1000).toLong() + 150) - now
            
            if (clockOffsetRolling == null) {
                clockOffsetRolling = currentInstantOffset.toDouble()
            } else {
                // 【異常值過濾】如果單次偏差大於 1 秒，判定為網路大延遲，不予學習
                if (abs(currentInstantOffset - clockOffsetRolling!!) < 1000) {
                    clockOffsetRolling = (1 - smoothingFactor) * clockOffsetRolling!! + smoothingFactor * currentInstantOffset
                }
            }

            // 2. 立即更新 remainingSeconds，確保 UI/Notification 讀取到最新值
            val adjustedNow = now.toDouble() + (clockOffsetRolling ?: 0.0)
            val diff = state.targetEndTimeUnix.toDouble() - adjustedNow
            _remainingSeconds.value = if (diff > 0) (diff / 1000.0) else 0.0
        } else {
            // 暫停狀態或無目標時間，直接使用 State 數值
            _remainingSeconds.value = state.remainingSeconds
        }

        // 3. 最後更新狀態旗標 (觸發 TimerService 的監聽器)
        _isWorkPhase.value = state.isWorkPhase
        _isPaused.value = state.isPaused
    }

    fun localTogglePause() { _isPaused.value = !_isPaused.value }
    fun localTogglePhase() {
        _isWorkPhase.value = !_isWorkPhase.value
        reset()
    }

    fun reset() {
        workDurationMinutes = nextWorkMins
        breakDurationMinutes = nextBreakMins
        val mins = if (_isWorkPhase.value) workDurationMinutes else breakDurationMinutes
        _remainingSeconds.value = mins * 60.0
        _isPaused.value = true
        lastState = null
        clockOffsetRolling = null
    }
}