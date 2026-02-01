# Specification: Android Power Optimization (v1.1.5)

## 1. Overview
本規格旨在降低 Android 端 App 在背景執行或螢幕關閉時的電力消耗。核心策略是減少 CPU 喚醒次數 (Waking up CPU) 以及移除不必要的高頻運算，同時確保在螢幕重新開啟或 App 回到前台時能精準顯示對時結果。

## 2. Functional Requirements

### 2.1 智慧延遲 (Smart Delay / Adaptive Ticking)
- **狀態偵測**: 整合 `androidx.lifecycle:lifecycle-process`，使用 `ProcessLifecycleOwner` 監測 App 全域生命週期。
- **動態調整 Ticking 頻率**:
    - **前台 (Foreground)**: 維持 50ms 延遲，以確保環形 UI 動畫流暢。
    - **背景 (Background)**: 延遲增加至 1000ms (1秒)，足以更新通知欄顯示。
    - **螢幕關閉 (Screen Off)**: 偵測到螢幕關閉時，完全停止本地 `remainingSeconds` 的更新計算迴圈 (Ticking Loop)。
- **即時恢復**: 當 App 回到前台或螢幕開啟時，立即根據 `targetEndTimeUnix` 重新計算並恢復 Ticking，確保使用者看到的數字是最新且正確的。

### 2.2 預約制音效 (Scheduled Sound Trigger)
- **機制轉換**: 移除原本每 50ms 監測一次 `remainingSeconds <= 0` 的輪詢 (Polling) 邏輯。
- **預約觸發**: 
    - 當階段開始 (Start) 或切換 (Switch Phase) 時，計算距離結束的剩餘毫秒數。
    - 使用 `CoroutineScope.launch { delay(remainingMs); playSound() }` 進行單次延遲觸發。
- **生命週期綁定**: 若 App 關閉或 Service 銷毀，該 Coroutine 應隨之取消，符合「App 關閉時不播放音效」的需求。

## 3. Non-Functional Requirements
- **省電效率**: CPU 使用率在 App 進入背景後應有顯著下降 (減少 20 倍以上的運算頻次)。
- **強健性**: 本地 Ticking 的降頻不應影響 SignalR 網路指令 (如暫停/恢復) 的接收反應速度。

## 4. Acceptance Criteria
- [ ] App 在前台時，UI 動畫保持流暢 (20fps+)。
- [ ] App 進入背景時，通知欄每秒跳動一次，未出現負數或跳秒異常。
- [ ] 螢幕關閉期間，確認 Log 中無持續的高頻計算。
- [ ] 螢幕重新開啟後，顯示的時間與電腦端保持同步且數值正確。
- [ ] 計時結束時，音效能準時觸發 (前提是 App/Service 仍在運行)。

## 5. Out of Scope
- **Doze Mode 喚醒**: 不使用 `AlarmManager` 強行在系統深度休眠 (Doze Mode)時喚醒播放。
- **UI 重構**: 本次更動僅限於底層 Logic 與 Service，不涉及介面設計。
