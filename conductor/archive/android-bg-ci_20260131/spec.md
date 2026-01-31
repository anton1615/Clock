# 軌跡規格書：Android 背景同步優化與 PC 啟動設定

#### 1. 概述 (Overview)
本軌跡旨在提升 Clock 專案的可用性與自動化程度。主要包含：為 Android 端加入背景通知與系統音效支援、為 PC 端加入控制台視窗切換設定，以及自動化 CI/CD 流程以支援雙平台（EXE/APK）發佈。

#### 2. 功能需求 (Functional Requirements)

**PC 端 (WPF):**
*   **控制台切換**：在設定視窗加入「顯示開發者控制台」勾選框。
*   **啟動邏輯**：
    *   若勾選（預設），啟動時開啟控制台黑框（現狀）。
    *   若取消勾選，啟動時僅顯示主視窗（WPF UI），隱藏後台黑框。

**Android 端 (Kotlin/Compose):**
*   **前台服務 (Foreground Service)**：實作背景服務以維持與 PC 的 SignalR 連線，確保 App 進入背景後不會中斷同步。
*   **系統通知欄倒數**：
    *   使用 Android 原生 \Chronometer\ API，在通知欄顯示實時倒數（MM:SS）。
    *   通知內容顯示當前階段名稱（工作/休息）。
    *   點擊通知可快速切換回 App 主畫面。
    *   通知強調色（Accent Color）隨階段變化（工作：橘色 / 休息：綠色）。
*   **階段切換音效**：當計時結束或手動切換階段時，調用 Android 系統預設的「通知 (Notification)」音效。

**CI/CD (GitHub Actions):**
*   **雙平台發佈**：
    *   保留現有的 Win-x64 單一執行檔編譯。
    *   新增 Android Job，自動編譯 Release 版 APK。
    *   發佈（Release）時，附件應同時包含 \clock.exe\ 與 \clock.apk\。

#### 3. 非功能需求 (Non-Functional Requirements)
*   **效能與省電**：背景倒數應利用系統 UI 渲染，避免 App 核心每秒喚醒 CPU。
*   **版權合規**：Android 端不內置任何音效檔，完全調用使用者手機系統資源。

#### 4. 驗收標準 (Acceptance Criteria)
*   [ ] PC 取消勾選控制台後重啟，不再出現 CMD 黑框。
*   [ ] 手機進入背景後，通知欄出現與主程式同步的倒數秒數。
*   [ ] 手機端切換階段時能聽到系統預設通知音。
*   [ ] GitHub 新 Tag 的 Release 頁面可下載到最新版的 APK。

#### 5. 超出範圍 (Out of Scope)
*   不實作自定義 Android 通知音效檔案上傳功能。
*   不改動目前的 EXE 打包結構（維持 Assets/ 資料夾外掛模式）。
