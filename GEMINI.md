# Gemini CLI 專案上下文彙總 (Clock)

## 專案現況
- **專案名稱**: Clock
- **目前版本**: v1.1.8 (Unified Target Architecture)
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
9. **統一目標架構 (v1.1.8)**:
   - **單一目標模型 (Single Target Model)**: 引擎不再跑內部循環，改為被動計算剩餘秒數 `TargetEndTime - Now`。徹底解決時間累積誤差與同步漂移。
   - **按需刷新 (On-Demand Tick)**: 
     - **前台**: UI 啟動 50ms 循環。
     - **背景**: Service 啟動 1s 循環（僅螢幕開啟時）。
     - **黑畫面**: 完全停止內部循環，0% CPU 佔用。
   - **媒體音量管理**: 改用 `MediaPlayer` 並設定 `USAGE_MEDIA`，使倒數音效完全受「媒體音量」滑桿控制。
   - **硬體喚醒與轉場**: 透過 `AlarmManager` 強行喚醒 Service 執行轉場，支援在黑畫面轉場時自動播放音效並**亮屏 3 秒**。
   - **通知欄 UI 鎖定**: 移除 `Chronometer` API，改由 Service 每秒更新靜態文字，嚴格執行 `Math.max(0.0)` 箝位保護，封殺負數顯示。
   - **服務生存強化**: 移除 `onTaskRemoved` 的 `stopSelf`。即使滑掉 App，計時、同步與鬧鐘仍會繼續運行。
   - **音效去重機制**: 實作 `lastPlayedTargetTime` 鎖定，解決背景恢復執行時的重複音效觸發問題。

## 重要技術決策
- **事件驅動轉場**: 將「計時」與「轉場」解耦。轉場由 OS 級鬧鐘驅動，保證在深度休眠下依然觸發。
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
