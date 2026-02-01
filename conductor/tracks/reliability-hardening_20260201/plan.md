# Plan: 背景精準度與 UI 修正

## 1. 問題描述
- **音效失效**：Android 螢幕關閉後進入 Doze Mode，協程的 `delay` 會被暫停，導致倒數結束不響鈴或延遲。
- **負數顯示**：PC 與手機對時微差導致 `remainingSeconds` 出現負值。

## 2. 解決方案
### A. AlarmManager 集成
- 使用 `setExactAndAllowWhileIdle` 預約 `TRIGGER_SOUND` Action。
- 在 `onStartCommand` 接收並播放音效。
- 每次重新預約前 `cancel` 舊的定時。

### B. 數值箝位
- 在 `PomodoroEngine` 的 `refreshTimeFromTarget` 與主循環中加入 `Math.max(0.0, ...)`。

## 3. 測試要點
- [x] 螢幕關閉 2 分鐘後確認音效是否準時。
- [x] 離線切換 Phase 是否會出現負數。
- [x] 與 PC 同步時手動更改時長，確認音效是否重對齊。
