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

    // 唯一事實來源：目標結束時間 (Unix Timestamp MS)
    private var targetEndTime: Long = 0L
    private var pausedRemainingSeconds: Double = workDurationMinutes * 60.0
    
    private var nextWorkMins: Int = workDurationMinutes
    private var nextBreakMins: Int = breakDurationMinutes

    private var clockOffsetRolling: Double? = null
    private val smoothingFactor = 0.15

    /**
     * 計算並更新目前的剩餘秒數 (被動觸發)
     */
    fun refreshRemainingSeconds(): Double {
        if (_isPaused.value) {
            _remainingSeconds.value = Math.max(0.0, pausedRemainingSeconds)
            return _remainingSeconds.value
        }

        val now = System.currentTimeMillis()
        val adjustedNow = now.toDouble() + (clockOffsetRolling ?: 0.0)
        val diff = targetEndTime.toDouble() - adjustedNow
        val result = Math.max(0.0, diff / 1000.0)
        _remainingSeconds.value = result
        return result
    }

    fun updateDurations(work: Int, breakM: Int) {
        nextWorkMins = work
        nextBreakMins = breakM
    }

    fun applyState(state: EngineState) {
        _isWorkPhase.value = state.isWorkPhase
        _isPaused.value = state.isPaused
        
        if (!state.isPaused && state.targetEndTimeUnix > 0) {
            // 同步模式下的對時補償
            val now = System.currentTimeMillis()
            val currentInstantOffset = (state.targetEndTimeUnix - (state.remainingSeconds * 1000).toLong()) - now
            
            if (clockOffsetRolling == null) {
                clockOffsetRolling = currentInstantOffset.toDouble()
            } else {
                if (Math.abs(currentInstantOffset - clockOffsetRolling!!) < 1000) {
                    clockOffsetRolling = (1 - smoothingFactor) * clockOffsetRolling!! + smoothingFactor * currentInstantOffset
                }
            }
            targetEndTime = state.targetEndTimeUnix
        } else {
            pausedRemainingSeconds = state.remainingSeconds
            targetEndTime = 0L
        }
        refreshRemainingSeconds()
    }

    fun localTogglePause() {
        val wasPaused = _isPaused.value
        
        if (!wasPaused) {
            // 從「執行」轉「暫停」：必須在切換狀態前，先算出當下剩餘秒數並保存
            val now = System.currentTimeMillis()
            val adjustedNow = now.toDouble() + (clockOffsetRolling ?: 0.0)
            val diff = targetEndTime.toDouble() - adjustedNow
            pausedRemainingSeconds = Math.max(0.0, diff / 1000.0)
            targetEndTime = 0L
        } else {
            // 從「暫停」轉「恢復」：設定新目標點
            targetEndTime = System.currentTimeMillis() + (pausedRemainingSeconds * 1000).toLong()
        }
        
        _isPaused.value = !wasPaused
        refreshRemainingSeconds()
    }

    fun localTogglePhase() {
        _isWorkPhase.value = !_isWorkPhase.value
        reset(startPaused = false)
    }

    fun reset(startPaused: Boolean = true) {
        workDurationMinutes = nextWorkMins
        breakDurationMinutes = nextBreakMins
        val mins = if (_isWorkPhase.value) workDurationMinutes else breakDurationMinutes
        
        pausedRemainingSeconds = mins * 60.0
        _isPaused.value = startPaused
        
        if (!startPaused) {
            targetEndTime = System.currentTimeMillis() + (pausedRemainingSeconds * 1000).toLong()
        } else {
            targetEndTime = 0L
        }
        clockOffsetRolling = null
        refreshRemainingSeconds()
    }

    fun getTargetEndTimeUnix(): Long = targetEndTime
    fun setSyncStatus(synced: Boolean) {
        _isSynced.value = synced
        if (!synced) {
            clockOffsetRolling = null
        }
    }
}