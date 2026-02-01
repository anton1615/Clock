# Gemini CLI 專案上下文彙總 (Clock)

## 專案現況
- **專案名稱**: Clock
- **目前版本**: v1.1.7 (Background Precision & Hardened Sync)
- **核心技術**: .NET 10, WPF, Android Native (Kotlin + Compose), SignalR, mDNS
- **開發日期**: 2026-02-01

## 專案結構
- clock: WPF 視圖層、通訊服務器實作與程式入口。支援 WinExe 模式與動態控制台分配。
- clock.Lib: 核心邏輯、模型與 ViewModel。已重構為純 .NET 10 Library，與 UI 完全解耦。
- clock-android: Android 原生專案 (Android Studio)，負責手機端同步顯示、服務與本地設定。
- Tests/clock.IntegrationTests: 整合測試，包含計時器、狀態同步、設定持久化與網路診斷測試。
- conductor/: AI 導引開發軌跡資料夾 (包含詳細的 Track 紀錄與 Plan/Spec)。

## 核心功能紀錄
1. **動態主題背景**: 背景顏色隨階段變動，邏輯：計算 Bar 顏色之 30% 亮度的深色調。
2. **Android 同步 (v1.1.0)**: 透過 mDNS 自動發現，使用 SignalR 實時同步。
3. **高精度計時**: PC 端採用 UTC 時間戳絕對座標，手機端使用 EMA 補償網路延遲與時鐘偏差。
4. **背景同步與通知 (v1.1.1)**: 使用 Foreground Service 維持連線，Chronometer API 顯示通知欄倒數。
5. **進階自訂 (v1.1.2)**: Android 端本地設定頁面，支援時長、顏色與系統音效選擇。
6. **安全性強化 (v1.1.3)**: PC 設定遷移至 LocalAppData，Android 加入 IP/Hostname 驗證。
7. **電力優化 (v1.1.5)**: Adaptive Ticking (前台 50ms / 背景 1s / 螢幕關閉 1s)。
8. **可靠性修復 (v1.1.6)**: 修正螢幕關閉時間凍結，實作音效重對齊 (Drift > 2s 重新預約)。
9. **背景與 UI 硬化 (v1.1.7)**:
   - **系統級音效預約**: 使用 `AlarmManager.setExactAndAllowWhileIdle` 解決 Doze 模式。
   - **音效去重機制**: 實作 `lastPlayedTargetTime` 去重，解決背景恢復時的重複音效。
   - **數值箝位 (Clamping)**: 徹底封殺 `00:-1` 顯示，通知欄小於 1s 自動轉為靜態。
   - **本地目標時間機制**: 改用 `localTargetEndTime` 模型，確保背景掛起後的恢復準確度。
   - **服務生存強化**: 移除 `onTaskRemoved` 的 `stopSelf`，確保滑掉 App 後計時繼續。
   - **黑畫面喚醒**: 實作 `wakeScreen(3s)`，在轉場音效播放時自動點亮螢幕。
   - **手動 Skip 靜音**: 加入剩餘秒數判定，確保手動跳過階段時不響鈴。
   - **安全轉場保險 (Safety Net)**: 在 `TimerService` 監聽秒數歸零。若 `AlarmManager` 延遲，Service 會強制播放音效並切換階段，防止卡在 0 秒。

## 重要技術決策
- **雙軌音效觸發 (Dual-Track)**: 
    - **協程 (Coroutine)**: 處理 App 活躍時的低延遲觸發（使用者空間輕量級執行緒）。
    - **AlarmManager**: 處理 App 休眠/凍結時的系統級喚醒（OS Kernel 服務）。
- **單一事實來源**: 以 PC 為同步基準，手機端執行 Round-trip 同步邏輯。
- **負數保護機制**: 針對分散式系統的時間不對稱性，在所有賦值路徑強制執行下限檢查。
- **Android 14 合規**: 明確宣告並處理 `SCHEDULE_EXACT_ALARM` 權限。

## 網路配置要求
- **連接埠**: TCP 8888 (SignalR)。
- **防火牆**: 需放行「公用網路」入站規則。

## 待辦事項/未來方向
- [ ] 優化 mDNS 解析在複雜路由器環境下的成功率。
- [ ] Android 鎖定畫面通知在不同廠牌 (如 Sony) 的顯示相容性優化。
- [ ] 考慮加入自訂透明度動畫。

---
*此文件由 Gemini CLI 自動生成，作為專案狀態的「永久記憶」。*
