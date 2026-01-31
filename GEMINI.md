# Gemini CLI 專案上下文彙總 (Clock)

## 專案現況
- **專案名稱**: Clock
- **目前版本**: v1.1.0-dev (Android Sync Update)
- **核心技術**: .NET 10, WPF, Android Native (Kotlin + Compose), SignalR, mDNS
- **開發日期**: 2026-01-31

## 專案結構
- `clock`: WPF 視圖層、通訊服務器實作與程式入口。
- `clock.Lib`: 核心邏輯、模型與 ViewModel。已重構為純 `.NET 10` Library，與 UI 完全解耦。
- `clock-android`: Android 原生專案 (Android Studio)，負責手機端同步顯示與控制。
- `Tests/clock.IntegrationTests`: 整合測試，包含計時器、狀態同步與網路診斷測試。
- `.github/workflows/release.yml`: GitHub Actions CI/CD 腳本。
- `conductor/`: AI 導引開發軌跡資料夾。

## 核心功能紀錄
1. **動態主題背景**: 
   - 背景顏色隨階段變動，邏輯：計算 Bar 顏色之 30% 亮度的深色調。
2. **現代化交互**: 
   - 支援 MouseOver 高亮、Path 繪製圖示、呼吸閃爍效果。
3. **GUI 設定視窗**: 
   - 包含選色器、系統字型選單、音效瀏覽與開機啟動設定。
4. **Android 同步 (v1.1.0 新增)**:
   - **自動發現**: 透過 mDNS 搜尋同 LAN 下的 PC。
   - **即時同步**: 使用 SignalR 達成低於 100ms 的時間與狀態對齊。
   - **環形 UI**: 手機端採用類系統計時器的圓形進度條設計。
   - **雙模式**: 支援連線同步模式與離線獨立計時模式。

## 重要技術決策
- **核心抽象化**: 將計時器 (`ITimer`)、音效 (`IAudioService`) 與 UI 控制 (`IUIService`) 介面化，確保 `clock.Lib` 可跨平台重用。
- **單一事實來源**: 採用 Round-trip 同步邏輯（方案 B），一切以 PC 端狀態為準，手機端作為遠端顯示與控制器。
- **Android 原生轉向**: 從 .NET MAUI 轉向 Android Studio (Kotlin)，以獲得更佳的效能、UI 流暢度與省電特性。
- **通訊相容性**: 強制使用 SignalR Long Polling 與明文傳輸 (`usesCleartextTraffic`)，並改用 Port 8888 確保防火牆穿透力。
- **網路對時**: 引入 `TargetEndTimeUnix` (UTC) 以補償網路傳輸延遲。

## 網路配置要求
- **連接埠**: 必須允許 TCP 8888 (SignalR) 與 UDP 5353 (mDNS)。
- **網路類型**: 建議設為「私人 (Private)」。支援 Tailscale 虛擬區域網路連線。

## 待辦事項/未來方向
- [ ] 實作 Android 端的「前台服務 (Foreground Service)」，防止 App 進入背景後被系統殺死。
- [ ] 優化 mDNS 解析 (Resolve) 成功率。
- [ ] 考慮加入自訂透明度動畫。

---
*此文件由 Gemini CLI 自動生成，作為 reload memory 的基礎。*