# Gemini CLI 專案上下文彙總 (Clock)

## 專案現況
- **專案名稱**: Clock
- **目前版本**: v1.0.2 (正式穩定版)
- **核心技術**: .NET 10, WPF, MVVM (CommunityToolkit.Mvvm)
- **開發日期**: 2026-01-20

## 專案結構
- `clock`: WPF 視圖層與程式入口。
- `clock.Lib`: 核心邏輯、模型與 ViewModel (UI-agnostic)。
- `Tests/clock.IntegrationTests`: 整合測試，確保引擎與設定穩定。
- `.github/workflows/release.yml`: GitHub Actions CI/CD 腳本，負責單一檔案發布 (Single-file EXE) 並排除 PDB。
- `conductor/`: AI 導引開發軌跡資料夾（已設定 `.gitignore` 排除追蹤，僅保留本地筆記）。

## 核心功能紀錄
1. **動態主題背景**: 
   - 背景顏色會隨 Work/Break 階段變動。
   - 邏輯：根據使用者設定的 Bar 顏色自動計算其 30% 亮度的深色調。
2. **現代化交互**: 
   - **高亮效果**: 進度條點擊區與 ⏩ 按鈕支援 MouseOver 視覺反饋。
   - **圖示化**: 跳過按鈕改為 Path 繪製的播放圖示。
3. **GUI 設定視窗強化**: 
   - **選色器**: 純 XAML 實作，包含質感預設色標、HEX 輸入與即時預覽。
   - **字型選單**: 自動載入系統內建字型清單供 ComboBox 選擇。
   - **音效瀏覽**: 新增 OpenFileDialog 按鈕，支援選取自定義 `.wav`。
4. **開機啟動 (Run at Startup)**: 
   - 實作於 `StartupService`，透過在啟動資料夾建立 `.lnk` 捷徑達成。
   - 設定介面具備勾選框，並會在開啟時主動檢查實際檔案狀態。
5. **佈局與縮放**: 
   - 視窗縮放支點固定於「底部中央 (Bottom-Center Anchor)」。
   - 預設 Work/Break 為 25/5 分鐘。

## 重要技術決策
- **極簡主義**: 堅持不使用第三方 UI 套件（如 WPF UI 或 Material Design），以維持 `.exe` 體積最小化。
- **安全與隱私**: 
  - 無 PII 收集，資料僅留本地。
  - 路徑處理採用 `Path.Combine` 等安全實作。
- **Git 管理**: 
  - 使用 `git rm --cached` 移除 `conductor/` 追蹤，確保私人開發筆記不外洩。
  - `.pdb` 符號檔已在發布流程中排除。

## 待辦事項/未來方向
- [ ] 考慮加入自訂透明度動畫。
- [ ] 監視 `Assets/notify.wav` 的檔案有效性。

---
*此文件由 Gemini CLI 自動生成，作為 reload memory 的基礎。*
