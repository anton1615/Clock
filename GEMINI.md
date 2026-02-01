# Gemini CLI 專案上下文彙總 (Clock)

## 專案現況
- **專案名稱**: Clock
- **目前版本**: v1.1.9 (Pixel Hardened & Low-Latency Audio)
- **核心技術**: .NET 10, WPF, Android Native (Kotlin + Compose), SignalR, mDNS
- **開發日期**: 2026-02-01

## 專案結構
- clock: WPF 視圖層、通訊服務器實作與程式入口。支援 WinExe 模式與動態控制台分配。
- clock.Lib: 核心邏輯、模型與 ViewModel。已重構為純 .NET 10 Library，與 UI 完全解耦。
- clock-android: Android 原生專案 (Android Studio)，負責手機端同步顯示、背景服務、本地設定與媒體播放。
- Tests/clock.IntegrationTests: 整合測試，包含計時器、狀態同步、設定持久化與網路診斷測試。
- conductor/: AI 導引開發軌跡資料夾 (包含詳細的 Track 紀錄與 Plan/Spec)。

## 核心功能紀錄
1. **動態主題背景**: 背景顏色隨階段變動，邏輯：計算 Bar 顏色之 30% 亮度的深色調。
2. **Android 同步 (v1.1.0)**: 透過 mDNS 自動發現，使用 SignalR 實時同步。
3. **高精度計時**: PC 端採用 UTC 時間戳絕對座標，手機端使用 EMA 補償網路延遲與時鐘偏差。
4. **背景同步與通知 (v1.1.1)**: 使用 Foreground Service 維持連線，更新通知列資訊。
5. **進階自訂 (v1.1.2)**: Android 端本地設定頁面，支援時長、顏色與系統音效選擇。
6. **安全性強化 (v1.1.3)**: PC 設定遷移至 LocalAppData，Android 加入 IP/Hostname 證。
7. **電力優化 (v1.1.5)**: Adaptive Ticking (前台 50ms / 背景亮屏 1s / 黑畫面 0s)。
8. **可靠性修復 (v1.1.6)**: 修正螢幕關閉時間凍結，實作音效重對齊 (Drift > 2s 重新預約)。
9. **統一目標架構 (v1.1.8)**: 引擎改為被動計算模式 `TargetEndTime - Now`。徹底消滅累積誤差與同步漂移。
10. **Pixel 硬化與低延遲音訊 (v1.1.9)**:
    - **AlarmClock API**: 升級轉場預約至 `setAlarmClock()`，保證在 Pixel 激進的節電模式下黑畫面轉場絕對準時。
    - **SoundPool 技術**: 捨棄 `MediaPlayer`，改用預載記憶體的 `SoundPool` 並路由至鬧鐘流。解決藍牙耳機播放串流音樂時發生的 10 秒音訊焦點競爭延遲。
    - **Android 14 權限自動化**: 導入 `USE_EXACT_ALARM` 並實作「三重保險」預約邏輯，保證在各版本 Android 上均不崩潰。
    - **通知分類與優先級**: 設定 `CATEGORY_ALARM` 並釘選通知，解決鎖屏時通知被收納消失的問題，同時恢復自定義底色。
    - **轉場安全保險**: Service 持續監聽秒數，若系統鬧鐘延遲，Service 會即時補位觸發轉場。

## 重要技術決策
- **事件驅動轉場**: 將「計時顯示」與「狀態轉場」完全解耦。轉場由 OS 級最高權限鬧鐘驅動。
- **單一事實來源**: 以物理時間（或 PC 傳來的目標戳記）為唯一對時基準。
- **負數保護機制**: 針對分散式系統的不對稱性，在引擎、UI、通知列三層級強制執行 `Math.max(0.0, ...)`。
- **Android 14 合規**: 明確宣告並處理 `SCHEDULE_EXACT_ALARM`、`USE_EXACT_ALARM` 與 `WAKE_LOCK` 權限。

## 網路配置要求
- **連接埠**: TCP 8888 (SignalR)。
- **防火牆**: 需放行「公用網路」入站規則。

## 待辦事項/未來方向
- [ ] 優化 mDNS 解析在複雜路由器環境下的成功率。
- [ ] Android 鎖定畫面通知在不同廠牌 (如 Sony) 的顯示相容性優化。
- [ ] 考慮加入自訂透明度動畫。

---
*此文件由 Gemini CLI 自動生成，作為專案狀態的「永久記憶」。*
