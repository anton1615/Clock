# Implementation Plan: android-sync_20260130

#### Phase 1: 核心邏輯跨平台化與抽象化 (Core Refactoring) [checkpoint: 66347bd]
*本階段目標是確保 clock.Lib 能在不依賴 WPF 的情況下於 Android 執行。*
- [x] Task: 抽象化計時器邏輯 797119c
    - [x] 定義 ITimer 介面，抽離 DispatcherTimer 的依賴。
    - [x] 在 clock.Lib 實作基於 System.Timers.Timer 的 StandardTimer。
    - [x] 在 WPF 專案實作 WpfTimer 作為適配器。
- [x] Task: 狀態同步準備 (State DTO) e63eda7
    - [x] 建立 EngineState 類別，包含時間、階段、暫停狀態等資訊。
    - [x] 在 PomodoroEngine 實作 GetState() 與 ApplyState() 方法。
- [x] Task: 清理跨平台相容性 79b2a51
    - [x] 檢查並移除 clock.Lib 中任何對 System.Windows 的引用。
    - [x] 抽離音效播放邏輯至 IAudioService。
    - [x] 將 WPF 專屬的 Brush/Color 邏輯移至 View 層的 Converter。
    - [x] 調整 clock.Lib.csproj 為純 net10.0 專案。
- [x] Task: Conductor - User Manual Verification 'Phase 1: Core Refactoring' (Protocol in workflow.md) 66347bd

#### Phase 2: PC 端通訊服務實作 (Server & mDNS)
*本階段目標是讓 PC 具備「被發現」與「同步數據」的能力。*
- [ ] Task: 整合 SignalR Hub 伺服器
    - [ ] 在 WPF 專案中啟動輕量級 WebHost 並建立 ClockHub。
    - [ ] 實作指令接收邏輯 (TogglePause, TogglePhase, Skip)。
- [ ] Task: 實作 mDNS 服務宣告
    - [ ] 整合 Makaretu.Dns 套件，在網路中廣播 _clock._tcp.local。
- [ ] Task: 狀態推送機制
    - [ ] 串接 PomodoroEngine 的變更事件，即時透過 SignalR 推送到所有客戶端。
- [ ] Task: Conductor - User Manual Verification 'Phase 2: PC Communication' (Protocol in workflow.md)

#### Phase 3: Android (MAUI) 客戶端開發
*本階段目標是打造 Android App 介面並達成同步。*
- [ ] Task: 建立 .NET MAUI 專案
    - [ ] 建立 Android 專案並引用 clock.Lib。
- [ ] Task: 實作 mDNS 發現與手動連線
    - [ ] 使用 Android 原生 NsdManager 搜尋 PC。
    - [ ] 實作手動輸入 IP (支援 Tailscale) 的 UI 與快取機制。
- [ ] Task: 實作環形計時器 UI
    - [ ] 參照 Android 系統計時器，實作圓形進度條與數位顯示。
- [ ] Task: 同步與獨立模式邏輯
    - [ ] 實作 SignalR 客戶端連線。
    - [ ] 處理斷線自動重置並降級為獨立模式。
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Android App' (Protocol in workflow.md)