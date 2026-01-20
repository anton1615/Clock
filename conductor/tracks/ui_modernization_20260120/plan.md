# Implementation Plan: UI/UX Modernization & Settings Extension

## Phase 1: Core Logic & Settings Model Extension [checkpoint: 6aa5c14]
- [x] **Task: Update `AppSettings` Model** a51e3f9
    - [ ] Add `IsStartupEnabled` boolean field.
    - [ ] Ensure `NotificationSoundPath` and other fields are properly mapped.
- [x] **Task: Implement Startup Shortcut Service** 0d6dd54
    - [ ] Add `StartupService` in `clock.Lib`.
    - [ ] Implement logic to create/delete shortcuts in `%AppData%\Microsoft\Windows\Start Menu\Programs\Startup`.
- [x] **Task: Test Settings & Startup Logic** 08b2b8b
    - [ ] Write unit tests to verify `AppSettings` serialization.
    - [ ] Verify `StartupService` correctly manages the file in the startup directory.
- [x] **Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)** 6aa5c14

## Phase 2: Main Window Visual Modernization
- [ ] **Task: Implement Dynamic Background Logic**
    - [ ] Add `CurrentBackgroundColor` property to `MainViewModel`.
    - [ ] Implement color conversion logic: darken the user-set Work/Break colors.
- [ ] **Task: Progress Bar Hover Effect (XAML Style)**
    - [ ] Update `MainWindow.xaml` to add a `Style` and `ControlTemplate` for the progress bar.
    - [ ] Use `Trigger` for `IsMouseOver` to change the foreground or opacity.
- [ ] **Task: Icon-based Skip Button**
    - [ ] Use `Path` to draw a ⏩ icon instead of text.
    - [ ] Apply modern button style (borderless, hover highlight).
- [ ] **Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)**

## Phase 3: Settings Window Enhancement (GUI Pickers)
- [ ] **Task: Audio File Picker**
    - [ ] Add "Browse" button in the settings window linked to `Microsoft.Win32.OpenFileDialog`.
- [ ] **Task: Font Family Picker**
    - [ ] Populate a ComboBox with `System.Windows.Media.Fonts.SystemFontFamilies`.
- [ ] **Task: Native Color Picker Integration**
    - [ ] Implement a mechanism to open a color picker (e.g., `System.Windows.Forms.ColorDialog` or a custom XAML picker) when clicking color preview areas.
- [ ] **Task: Integration Test for Settings UI**
    - [ ] Ensure UI values correctly write back to `AppSettings` and persist to disk.
- [ ] **Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)**
