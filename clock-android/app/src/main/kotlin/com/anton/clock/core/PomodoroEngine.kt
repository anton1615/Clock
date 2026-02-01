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
    private var localTargetEndTime: Long = 0L

    fun setForeground(foreground: Boolean) { 
        val changed = _isForeground.value != foreground
        _isForeground.value = foreground 
        if (changed) {
            refreshTimeFromTarget()
            start() // 重啟 Loop 以更新 Delay 並中斷舊延遲
        }
    }
    
    fun setScreenOn(on: Boolean) { 
        val changed = _isScreenOn.value != on
        _isScreenOn.value = on 
        if (changed) {
            refreshTimeFromTarget()
            start() // 重啟 Loop 以更新 Delay 並中斷舊延遲
        }
    }

    fun updateDurations(work: Int, breakM: Int) {
        nextWorkMins = work
        nextBreakMins = breakM
    }

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    
    private var lastTime: Long = 0
    private var clockOffsetRolling: Double? = null
    private val smoothingFactor = 0.15 // 降低一點權重，更穩定
    private var lastState: EngineState? = null

    /**
     * 核心對時邏輯：根據 TargetEndTime 與本地時間差計算剩餘秒數
     */
    private fun refreshTimeFromTarget() {
        if (_isSynced.value && lastState != null && !lastState!!.isPaused && lastState!!.targetEndTimeUnix > 0) {
            val now = System.currentTimeMillis()
            val adjustedNow = now.toDouble() + (clockOffsetRolling ?: 0.0)
            val diff = lastState!!.targetEndTimeUnix.toDouble() - adjustedNow
            _remainingSeconds.value = Math.max(0.0, diff / 1000.0)
        } else if (!_isPaused.value && localTargetEndTime > 0) {
            val now = System.currentTimeMillis()
            val diff = (localTargetEndTime - now) / 1000.0
            _remainingSeconds.value = Math.max(0.0, diff)
        }
    }

    fun start() {
        timerJob?.cancel() // 這是關鍵：取消舊的延遲任務
        
        // 如果是全新的開始，初始化 lastTime
        if (lastTime == 0L) {
            lastTime = System.currentTimeMillis()
        }

        timerJob = scope.launch {
            while (isActive) {
                // 動態調整 Delay
                val currentDelay = when {
                    !_isScreenOn.value -> 1000L // 螢幕關閉時，每 1 秒醒來一次，提升亮屏時的時間準確感
                    _isPaused.value -> 500L
                    !_isForeground.value -> 1000L // 後台時，1 秒
                    else -> 50L // 前台時，50ms
                }
                
                delay(currentDelay)
                
                val now = System.currentTimeMillis()
                
                if (_isSynced.value && lastState != null) {
                    refreshTimeFromTarget()
                } else if (!_isPaused.value) {
                    val delta = (now - lastTime) / 1000.0
                    val newVal = _remainingSeconds.value - delta
                    if (newVal > 0) {
                        _remainingSeconds.value = newVal
                    } else {
                        // 倒數結束，自動切換階段 (離線模式)
                        _remainingSeconds.value = 0.0
                        lastTime = now // 更新時間基準，避免切換後 delta 累計
                        localTogglePhase()
                        return@launch // 跳出舊 Loop，localTogglePhase 會啟動新的
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
        val pauseChanged = _isPaused.value != state.isPaused
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

            // 2. 立即更新 remainingSeconds
            refreshTimeFromTarget()
        } else {
            // 暫停狀態或無目標時間，直接使用 State 數值
            _remainingSeconds.value = Math.max(0.0, state.remainingSeconds)
        }

        // 3. 最後更新狀態旗標 (觸發 TimerService 的監聽器)
        _isWorkPhase.value = state.isWorkPhase
        _isPaused.value = state.isPaused

        // 4. 如果從暫停變為執行，重置 lastTime 避免時間跳變
        if (pauseChanged) {
            if (!state.isPaused) lastTime = System.currentTimeMillis()
            start()
        }
    }

    fun localTogglePause() { 
        val newPaused = !_isPaused.value
        _isPaused.value = newPaused
        if (!newPaused) {
            lastTime = System.currentTimeMillis() // 恢復執行前重置時間點
            localTargetEndTime = System.currentTimeMillis() + (_remainingSeconds.value * 1000).toLong()
        } else {
            localTargetEndTime = 0L
        }
        start() // 切換暫停時重啟 Loop
    }

    fun localTogglePhase() {
        _isWorkPhase.value = !_isWorkPhase.value
        reset(startPaused = false) // 手動或自動切換階段後應自動開始
    }

    fun reset(startPaused: Boolean = true) {
        workDurationMinutes = nextWorkMins
        breakDurationMinutes = nextBreakMins
        val mins = if (_isWorkPhase.value) workDurationMinutes else breakDurationMinutes
        _remainingSeconds.value = Math.max(0.0, mins * 60.0)
        _isPaused.value = startPaused
        lastState = null
        clockOffsetRolling = null
        
        if (!startPaused) {
            localTargetEndTime = System.currentTimeMillis() + (_remainingSeconds.value * 1000).toLong()
        } else {
            localTargetEndTime = 0L
        }
        
        start() // 確保重啟 Loop
    }

    /**
     * 獲取當前有效的目標結束時間 (Unix MS)
     */
    fun getTargetEndTimeUnix(): Long {
        return if (_isSynced.value && lastState != null) {
            lastState!!.targetEndTimeUnix
        } else if (!_isPaused.value && localTargetEndTime > 0) {
            localTargetEndTime
        } else {
            0L
        }
    }
}