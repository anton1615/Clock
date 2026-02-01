# Implementation Plan: Android Power Optimization (v1.1.5)

## Phase 1: Infrastructure & Lifecycle Management [checkpoint: 8b703fa]
- [x] Task: Android - Add `androidx.lifecycle:lifecycle-process` dependency to `build.gradle.kts`. [93bd5c2]
- [x] Task: Android - Create `AppLifecycleObserver` class implementing `LifecycleEventObserver`. [aa28168]
    - [x] Implement `onStateChanged` to detect `ON_START` and `ON_STOP`.
    - [x] Create a singleton or shared mechanism to expose `isForeground` state to `PomodoroEngine`.
- [x] Task: Android - Integrate `ProcessLifecycleOwner` in `TimerApplication`. [a0f4ee1]
- [x] Task: Conductor - User Manual Verification 'Infrastructure & Lifecycle Management' (Protocol in workflow.md) [checkpoint: 8b703fa]

## Phase 2: Core Logic Optimization (Adaptive Ticking) [checkpoint: bd4d6a0]
- [x] Task: Android - Refactor `PomodoroEngine.kt` to accept `isForeground` and `isScreenOn` states. [365c896]
    - [x] Modify the main `while(isActive)` loop to dynamically adjust `delay()`.
    - [x] Implement logic: 50ms (Foreground), 1000ms (Background), Pause/Stop (Screen Off).
- [x] Task: Android - Implement Screen Off/On detection in `TimerService`. [b4cffd2]
    - [x] Register `Intent.ACTION_SCREEN_OFF` and `Intent.ACTION_SCREEN_ON` receivers.
    - [x] Pass screen state to `PomodoroEngine`.
- [x] Task: Android - Implement "Fast Forward" logic on resume. [a6f097b]
    - [x] When engine resumes from Screen Off or Background, force an immediate recalculation of `remainingSeconds` based on `targetEndTimeUnix`.
- [x] Task: Conductor - User Manual Verification 'Core Logic Optimization (Adaptive Ticking)' (Protocol in workflow.md) [checkpoint: bd4d6a0]

## Phase 3: Sound Scheduling (Zero-Wakeup) [checkpoint: b6e6e0a]
- [x] Task: Android - Implement `scheduleSound()` function in `TimerService`. [b6e6e0a]
    - [x] Calculate duration to `targetEndTimeUnix`.
    - [x] Launch a `Coroutine` with `delay(duration)` to play sound.
    - [x] Ensure this coroutine is cancelled if `isPaused` becomes true or phase changes.
- [x] Task: Android - Remove polling logic from `TimerService`. [b6e6e0a]
    - [x] Delete the `collect { remainingSeconds }` block that triggers sound.
- [x] Task: Conductor - User Manual Verification 'Sound Scheduling (Zero-Wakeup)' (Protocol in workflow.md) [checkpoint: b6e6e0a]

## Phase 4: Final Verification & Cleanup [checkpoint: b6e6e0a]
- [x] Task: Android - Manual Verification of Battery Usage (Log inspection). [b6e6e0a]
- [x] Task: Android - Verify Notification consistency in background. [b6e6e0a]
- [x] Task: Android - Verify Sound plays correctly when screen is off (via Scheduler). [b6e6e0a]
- [x] Task: Conductor - User Manual Verification 'Final Verification & Cleanup' (Protocol in workflow.md) [checkpoint: b6e6e0a]
