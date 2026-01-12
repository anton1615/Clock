# 技術棧 - Clock

## 核心環境
*   **程式語言**: C# (12.0+)
*   **運行環境**: .NET 10.0
*   **介面框架**: WPF (Windows Presentation Foundation)

## 核心套件 (Core Libraries)
*   **MVVM 工具**: `CommunityToolkit.Mvvm`
    *   用途: 提供 ObservableObject, RelayCommand 等標準 MVVM 實作。
*   **系統匣圖示**: `H.NotifyIcon.Wpf`
    *   用途: 實作 Windows 系統匣 (System Tray) 整合與通知。

## 資料與配置
*   **設定檔格式**: JSON
    *   主檔案: `setting.json` (位於應用程式目錄)
*   **資源管理**: 
    *   `Assets/`: 存放靜態資源如圖示 (.ico) 與音效 (.wav)。
    *   **服務解耦**: 使用 `ISettingsService` 處理跨專案的 UI 導覽。

## 開發規範
*   **架構模式**: MVVM (Model-View-ViewModel)
*   **非同步處理**: 廣泛使用 `async/await` 與 `DispatcherTimer` 進行 UI 執行緒安全的計時操作。
*   **UI 樣式**: XAML 定義，結合 `DataBinding` 與 `Converters` 處理複雜的視覺邏輯（如進度條顏色與寬度轉換）。
