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

    private var nextWorkMins: Int = workDurationMinutes
    private var nextBreakMins: Int = breakDurationMinutes

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
                val currentDelay = if (_isPaused.value) 500L else 50L
                delay(currentDelay)
                
                val now = System.currentTimeMillis()
                
                if (_isSynced.value && lastState != null) {
                    val state = lastState!!
                    if (!state.isPaused && state.targetEndTimeUnix > 0) {
                        val adjustedNow = now.toDouble() + (clockOffsetRolling ?: 0.0)
                        val diff = state.targetEndTimeUnix.toDouble() - adjustedNow
                        _remainingSeconds.value = if (diff > 0) (diff / 1000.0) else 0.0
                    } else {
                        _remainingSeconds.value = state.remainingSeconds
                    }
                } else {
                    val delta = (now - lastTime) / 1000.0
                    if (!_isPaused.value && _remainingSeconds.value > 0) {
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
        _isWorkPhase.value = state.isWorkPhase
        _isPaused.value = state.isPaused
        
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
        }
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