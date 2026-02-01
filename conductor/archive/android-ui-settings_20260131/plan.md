# Implementation Plan: android-ui-settings_20260131

#### Phase 1: 通知欄視覺與狀態同步強化
- [x] Task: 重新設計通知欄佈局與視覺
    - [ ] 修改 \TimerService.kt\ 以支援 \VISIBILITY_PUBLIC\ (鎖定畫面可見)
    - [ ] 實作強調色暗色化邏輯，並確保文字為亮色
    - [ ] 更新通知內容以包含「暫停狀態」與「同步狀態 (Sync)」的文字提示
- [x] Task: 修正通知欄 Chronometer 同步邏輯
    - [ ] 確保暫停時倒數停止，恢復時重新計算目標時間
- [ ] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)

#### Phase 2: Android 本地設定系統與 UI
- [x] Task: 實作設定資料持久化 (Data Persistence)
    - [ ] 建立 \SettingsRepository.kt\ 使用 \SharedPreferences\ 儲存時長與顏色設定
    - [ ] 編寫單元測試驗證設定的讀寫正確性
- [x] Task: 建立設定頁面 UI (Jetpack Compose)
    - [ ] 實作時長調整元件、顏色選擇器與開關
    - [ ] 實作從主畫面跳轉至設定頁的導覽邏輯
- [x] Task: 整合設定至計時引擎
    - [ ] 修改 \PomodoroEngine\ 以在階段切換時讀取新設定
- [x] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)

#### Phase 3: 音效與螢幕管理
- [~] Task: 音效選單與靜音邏輯
    - [ ] 在設定頁加入系統通知音效選擇器
    - [ ] 實作「無音效」邏輯，並在階段結束時調用正確音軌
- [x] Task: 實作螢幕常亮 (Keep Screen On) 功能
    - [ ] 根據設定值動態調整 \Window\ 的 \FLAG_KEEP_SCREEN_ON\
- [ ] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)

#### Phase 4: 圖示更新與最終發佈
- [ ] Task: 更新應用程式圖示
    - [ ] 將 \@app.png\ 轉換為 Android 適用的各尺寸資源檔 (Mipmaps)
    - [ ] 驗證手機桌面圖示顯示正確
- [ ] Task: Conductor - User Manual Verification 'Phase 4' (Protocol in workflow.md)






