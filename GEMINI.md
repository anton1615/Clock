# Gemini CLI 專案上下文彙總 (Clock)

## 專案現況
- **專案名稱**: Clock
- **目前版本**: v1.1.0 (Android Sync & High-Precision Update)
- **核心技術**: .NET 10, WPF, Android Native (Kotlin + Compose), SignalR, mDNS
- **開發日期**: 2026-01-31

## 專案結構
- `clock`: WPF 視圖層、通訊服務器實作與程式入口。
- `clock.Lib`: 核心邏輯、模型與 ViewModel。已重構為純 `.NET 10` Library，與 UI 完全解耦。
- `clock-android`: Android 原生專案 (Android Studio)，負責手機端同步顯示與控制。
- `Tests/clock.IntegrationTests`: 整合測試，包含計時器、狀態同步與網路診斷測試。
- `conductor/`: AI 導引開發軌跡資料夾。

## 核心功能紀錄
1. **動態主題背景**: 背景顏色隨階段變動，邏輯：計算 Bar 顏色之 30% 亮度的深色調。
2. **Android 同步 (v1.1.0)**:
   - **自動發現**: 透過 mDNS 搜尋同 LAN 下的 PC。
   - **即時同步**: 使用 SignalR (Port 8888) 達成低於 100ms 的時間與狀態對齊。
   - **環形 UI**: 手機端採用類系統計時器的圓形進度條設計。
   - **雙模式**: 支援連線同步模式與離線獨立計時模式。
3. **高精度計時**: PC 端改用 UTC 時間戳絕對座標計算，徹底解決 DispatcherTimer 導致的 Drift 偏差。

## 重要技術決策
- **核心抽象化**: 將計時器 (`ITimer`)、音效 (`IAudioService`) 與 UI 控制 (`IUIService`) 介面化，確保 `clock.Lib` 可跨平台重用。
- **單一事實來源**: 採用 Round-trip 同步邏輯（方案 B），一切以 PC 端狀態為準，手機端作為遠端顯示與控制器。
- **Android 原生轉向**: 從 .NET MAUI 轉向 Android Studio (Kotlin)，以獲得更佳的效能、UI 流暢度與省電特性。
- **網路對時**: 引入 `TargetEndTimeUnix` (UTC) 以補償網路傳輸延遲，手機端使用 EMA (指數移動平均) 進行平滑校正。

## 網路配置要求
- **連接埠**: 必須允許 TCP 8888 (SignalR)。
- **防火牆**: 在「公用網路」下需手動在進階防火牆中勾選「公用」設定檔以放行入站連線。

## 待辦事項/未來方向
- [ ] 實作 Android 端的「前台服務 (Foreground Service)」，防止 App 進入背景後被系統殺死。
- [ ] 優化 mDNS 解析 (Resolve) 在不同路由器下的成功率。
- [ ] 考慮加入自訂透明度動畫。

---
*此文件由 Gemini CLI 自動生成，作為 reload memory 的基礎。*
