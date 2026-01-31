# Track Spec: Project Maintenance and Verification

## 目標
對現有的 Clock 專案進行全面的維護性檢查，確保其程式碼架構、配置處理與資源管理符合 Conductor 設定的技術規範。

## 查核要點
1.  **MVVM 架構驗證**：
    *   檢查 `ViewModels/MainViewModel.cs` 是否正確實作 `ObservableObject` 並使用 `RelayCommand`。
    *   確認 `Views/MainWindow.xaml` 僅透過 DataBinding 與 ViewModel 溝通，無過多 Code-behind。
2.  **配置處理審計**：
    *   驗證 `Models/AppSettings.cs` 與 `setting.json` 的對應關係。
    *   確認音效路徑是否如方針所述從設定檔讀取。
3.  **Widget 特性驗證**：
    *   檢查 `MainWindow` 的視窗樣式（無標題欄、置頂邏輯）。
    *   驗證 `DispatcherTimer` 的計時準確性與執行緒安全。
4.  **資源引用**：
    *   確保 `Assets/` 下的圖示與音效被正確引用。

## 成功標準
*   所有核心類別皆經過人工審閱且無明顯架構偏離。
*   `setting.json` 運作正常，手動修改能反映在程式行為中。
*   符合 `conductor/workflow.md` 中定義的 80% 測試覆蓋率（若有實作測試單元）。
