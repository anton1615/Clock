# Gemini CLI 專案上下文彙總 (Clock)

## 專案現況
- **專案名稱**: Clock
- **目前版本**: v1.1.11 (Phase Transition Sound Fix)
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
   - **系統級音效預約**: 使用 `AlarmManager.setExactAndAllowWhileIdle` 解決 Doze 模式下協程被凍結導致的漏響問題。
   - **音效去重機制**: 實作 `lastPlayedTargetTime` 去重。當 App 從背景恢復時，若 Coroutine 與 AlarmManager 觸發時間點重疊，會自動跳過重複音效。
   - **數值箝位 (Clamping)**: 全面加入 `Math.max(0.0, ...)`，消除同步時產生的負數顯示與 `-1` 閃爍。
   - **本地目標時間機制**: Android Engine 改用 `localTargetEndTime` 模型，確保本地模式下背景掛起後的恢復準確度。
   - **服務生存強化**: 移除 `onTaskRemoved` 中的 `stopSelf`。即使使用者滑掉 App，Foreground Service 仍會維持運作。
   - **Drift 門檻最佳化**: 引入 2 秒 Drift 門檻（基於絕對目標時間），降低 `AlarmManager` 更新頻率。
10. **階段轉場音效修正 (v1.1.11)**:
    - **移除引擎自切換**: 解決 `PomodoroEngine` 內部循環與 `TimerService` 鬧鐘的競爭問題，防止因 Loop 搶先切換導致即將播放的音效任務被 Cancel。
    - **轉場偵測播放**: 在 `isWorkPhase` 監聽器中實作 `lastObservedPhase` 檢查，確保無論是本地計時到期、手動 Skip 還是 PC 端同步切換，都能正確觸發上一階段的結束音效。
    - **順序保證**: 嚴格執行「播放音效 -> 切換階段 -> 預約下一次」的執行序鏈，徹底解決黑畫面切換時無聲的問題。

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
