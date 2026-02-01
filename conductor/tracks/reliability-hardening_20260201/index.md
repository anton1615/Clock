# Track: 背景精準度與 UI 修正 (Background Precision & UI Fixes)

## 概述
針對 Android 平台在螢幕關閉（Doze Mode）下的計時與音效行為進行最終強化，並修正 UI 同步時的負數顯示瑕疵。

## 核心內容
1. **系統級定時 (AlarmManager)**：確保音效準時。
2. **負數箝位 (Clamping)**：消除 `-1` 顯示。
3. **Android 14 合規**：新增精準定時權限。

## 狀態
- [x] 設計方案
- [x] 代碼實作
- [x] 自我測試
- [x] 文件更新
