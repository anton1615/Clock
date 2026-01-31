# 技術棧說明 - Clock

## 核心開發環境
*   **語言**: C# (12.0+)
*   **框架**: .NET 10.0
*   **UI 框架**: WPF (Windows Presentation Foundation)

## 核心函式庫 (Core Libraries)
*   **MVVM 框架**: `CommunityToolkit.Mvvm`
    *   用於實作 ObservableObject, RelayCommand 等 MVVM 標準模式。
*   **系統托盤支援**: `H.NotifyIcon.Wpf`
    *   用於實作 Windows 系統托盤 (System Tray) 圖示與右鍵選單。

## 資料與設定
*   **設定儲存**: JSON
    *   設定儲存於 `setting.json` (位於 `%LocalAppData%\Clock` 下，支援自動遷移)。
*   **開機啟動管理**：透過 `StartupService` 於 Windows 啟動資料夾實作捷徑管理邏輯。
*   **服務解耦**: 使用 `ISettingsService` 處理跨專案的 UI 導覽。

## 資源管理
*   `Assets/`: 存放應用程式圖示 (.ico) 與提示音效 (.wav)。

## 架構設計
*   **模式**: MVVM (Model-View-ViewModel)
*   **非同步處理**: 採用 `async/await` 處理非同步邏輯，並使用 `DispatcherTimer` 驅動 UI 時間更新。
*   **自定義 XAML 樣式**：堅持不引入大型 UI 框架，完全透過 WPF `ControlTemplate`、`Triggers` 與 `VisualStateManager` 實作現代化互動效果，維持極輕量的執行檔體積。
*   **動態色彩計算**：實作顏色轉換邏輯，根據使用者選色自動衍生背景深色調。


## Android 銵ㄖ
*   **Foreground Service**: 蝣箔駁郊嚗Ⅱ靽 PC 駁銝駁
*   **Chronometer API**: 雿輻蝟餌絞原生 API 內嚗鼠拐蝙刻瘜
*   **RingtoneManager**: 隤輻蝟餌絞批遣單嚗蕭瘙Ｘ瞏

\
## 安全性與隱私
*   **傳輸安全**: 使用 SignalR 進行本地同步，CORS 政策僅開放給必要連線。
*   **輸入驗證**: Android 端實施嚴格的 IP/Hostname 驗證與顏色格式驗證。
*   **隱私保護**: Android 通知設定為 PRIVATE，隱藏鎖屏資訊。\
