# Implementation Plan: Android Power Optimization (v1.1.5)

## Phase 1: Infrastructure & Lifecycle Management
- [x] Task: Android - Add `androidx.lifecycle:lifecycle-process` dependency to `build.gradle.kts`. [93bd5c2]
- [x] Task: Android - Create `AppLifecycleObserver` class implementing `LifecycleEventObserver`. [aa28168]
    - [x] Implement `onStateChanged` to detect `ON_START` and `ON_STOP`.
    - [x] Create a singleton or shared mechanism to expose `isForeground` state to `PomodoroEngine`.
- [x] Task: Android - Integrate `ProcessLifecycleOwner` in `TimerApplication`. [a0f4ee1]
- [ ] Task: Conductor - User Manual Verification 'Infrastructure & Lifecycle Management' (Protocol in workflow.md) [checkpoint: ]

## Phase 2: Core Logic Optimization (Adaptive Ticking)
- [ ] Task: Android - Refactor `PomodoroEngine.kt` to accept `isForeground` and `isScreenOn` states.
    - [ ] Sub-task: Modify the main `while(isActive)` loop to dynamically adjust `delay()`.
    - [ ] Sub-task: Implement logic: 50ms (Foreground), 1000ms (Background), Pause/Stop (Screen Off).
- [ ] Task: Android - Implement Screen Off/On detection in `MainActivity` (or `TimerService`).
    - [ ] Sub-task: Register `Intent.ACTION_SCREEN_OFF` and `Intent.ACTION_SCREEN_ON` receivers.
    - [ ] Sub-task: Pass screen state to `PomodoroEngine`.
- [ ] Task: Android - Implement "Fast Forward" logic on resume.
    - [ ] Sub-task: When engine resumes from Screen Off or Background, force an immediate recalculation of `remainingSeconds` based on `targetEndTimeUnix`.
- [ ] Task: Conductor - User Manual Verification 'Core Logic Optimization (Adaptive Ticking)' (Protocol in workflow.md) [checkpoint: ]

## Phase 3: Sound Scheduling (Zero-Wakeup)
- [ ] Task: Android - Implement `scheduleSound()` function in `TimerService`.
    - [ ] Sub-task: Calculate duration to `targetEndTimeUnix`.
    - [ ] Sub-task: Launch a `Coroutine` with `delay(duration)` to play sound.
    - [ ] Sub-task: Ensure this coroutine is cancelled if `isPaused` becomes true or phase changes.
- [ ] Task: Android - Remove polling logic from `TimerService`.
    - [ ] Sub-task: Delete the `collect { remainingSeconds }` block that triggers sound.
- [ ] Task: Conductor - User Manual Verification 'Sound Scheduling (Zero-Wakeup)' (Protocol in workflow.md) [checkpoint: ]

## Phase 4: Final Verification & Cleanup
- [ ] Task: Android - Manual Verification of Battery Usage (Log inspection).
- [ ] Task: Android - Verify Notification consistency in background.
- [ ] Task: Android - Verify Sound plays correctly when screen is off (via Scheduler).
- [ ] Task: Conductor - User Manual Verification 'Final Verification & Cleanup' (Protocol in workflow.md) [checkpoint: ]
