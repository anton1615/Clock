# Implementation Plan: android-bg-ci_20260131

#### Phase 1: PC 端控制台切換功能
- [x] Task: 擴充設定模型與 UI 介面
    - [ ] 在 \AppSettings.cs\ 加入 \ShowConsole\ 布林屬性
    - [ ] 在 \SettingsWindow.xaml\ 加入對應的 CheckBox 勾選框
    - [ ] 實作資料綁定與自動存檔邏輯
- [x] Task: 實作動態控制台分配邏輯
    - [ ] 修改 \App.xaml.cs\ 的啟動程序
    - [ ] 實作基於設定值的 \AllocConsole\ (Win32 API) 調用
    - [ ] 驗證在取消勾選時，啟動不再跳出黑框
- [x] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)

#### Phase 2: Android 背景通知與倒數功能
- [x] Task: 實作 Android 前台服務 (Foreground Service)
    - [ ] 建立 \TimerService.kt\ 繼承自 \Service\
    - [ ] 在 \AndroidManifest.xml\ 註冊服務與必要權限 (FOREGROUND_SERVICE)
    - [ ] 將 \SignalRManager\ 的連線邏輯移入 Service 管理
- [x] Task: 實作通知欄實時倒數 (Chronometer)
    - [ ] 實作 \NotificationHelper\ 建立通知管道 (Channel)
    - [ ] 使用 \setUsesChronometer(true)\ 與 \setChronometerCountDown(true)\
    - [ ] 根據 \PomodoroEngine\ 狀態動態更新通知的 \ContentTitle\ 與 \AccentColor\
- [x] Task: 處理 App 生命週期與服務繫結
    - [ ] 在 \MainActivity\ 實作 Service 啟動與綁定邏輯
    - [ ] 確保 App 進入背景時通知持續顯示，點擊可返回 App
- [x] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)

#### Phase 3: Android 系統音效集成
- [x] Task: 實作階段切換音效觸發
    - [ ] 在 \PomodoroEngine\ 或 Service 中監聽階段變更事件
    - [ ] 使用 \RingtoneManager\ 獲取系統預設 \TYPE_NOTIFICATION\ 並播放
    - [ ] 驗證手動 Skip 或倒數結束時音效皆能觸發
- [x] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)

#### Phase 4: GitHub Actions 自動化與編譯優化
- [x] Task: 更新發佈流程支援 APK 編譯
    - [ ] 在 \elease.yml\ 加入 Android 編譯 Job (JDK 17, Gradle)
    - [ ] 設定編譯產出為 \pp-release.apk\
- [x] Task: 優化 PC 端單一執行檔輸出
    - [ ] 調整 \dotnet publish\ 參數以減少冗餘檔案
    - [ ] 確保 Release 附件同時包含 \clock.exe\ 與 \clock.apk\
- [x] Task: Conductor - User Manual Verification 'Phase 4' (Protocol in workflow.md)






