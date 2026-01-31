# Specification: Android Sync & High-Precision Engine

## 1. Overview
本功能旨在為 Clock 增加 Android 手機同步能力。透過 mDNS 進行服務發現，並利用 SignalR 達成電腦與手機間的毫秒級狀態同步。

## 2. Functional Requirements
### 2.1 Service Discovery (mDNS)
- **PC 端**：自動偵測所有實體網卡 IP（排除虛擬網卡如 VMware），廣播 `_clock._tcp.local`。
- **Android 端**：自動持續掃描並顯示可連線的 PC 列表。

### 2.2 Synchronization Logic (SignalR)
- **單一事實來源 (Source of Truth)**：連線狀態下，一切邏輯以 PC 端的 `PomodoroEngine` 為準。
- **高精度校準**：
    - PC 端：改用 UTC 時間戳絕對座標計算，消除 `DispatcherTimer` 導致的 Drift 偏移。
    - 手機端：實作 **EMA (指數移動平均) 滾動校準**，自動計算手機與電腦的系統時間差，並加入 150ms 網路延遲補償。
- **連接埠**：固定使用 **Port 8888** 以獲得最佳相容性。

### 2.3 Android UI/UX
- **介面**：使用 Jetpack Compose 實作。包含環形進度條與動態按鈕圖示。
- **播放/暫停提示**：時間文字下方顯示狀態圖示，按鈕內容隨狀態動態變化。
- **離線運作**：支援 Standalone 獨立模式，啟動預設為暫停。

## 3. Non-Functional Requirements
- **節能設計**：暫停時自動降頻廣播與採樣（PC 10s / 手機 0.5s）。
- **抗干擾**：具備 1 秒閾值的異常值過濾器 (Outlier Drop)，防止網路突發抖動。

## 4. Acceptance Criteria
1. 手機開啟後能自動在 10 秒內發現並連線至同 LAN 或 Tailscale PC。
2. 連線後兩端秒數跳動需在視覺上保持一致（誤差 < 100ms）。
3. PC 關閉後，手機需自動斷開並恢復獨立計時模式。
