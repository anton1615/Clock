package com.anton.clock.models

data class EngineState(
    val remainingSeconds: Double = 0.0,
    val isWorkPhase: Boolean = true,
    val isPaused: Boolean = false,
    val phaseName: String = "",
    val totalDurationSeconds: Double = 0.0,
    val targetEndTimeUnix: Long = 0L
)