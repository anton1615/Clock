# Implementation Plan: UI/UX Modernization & Settings Extension

## Phase 1: Core Logic & Settings Model Extension [checkpoint: 6aa5c14]
- [x] **Task: Update `AppSettings` Model** a51e3f9
    - [x] Add `IsStartupEnabled` boolean field.
    - [x] Ensure `NotificationSoundPath` and other fields are properly mapped.
- [x] **Task: Implement Startup Shortcut Service** 0d6dd54
    - [x] Add `StartupService` in `clock.Lib`.
    - [x] Implement logic to create/delete shortcuts in `%AppData%\Microsoft\Windows\Start Menu\Programs\Startup`.
- [x] **Task: Test Settings & Startup Logic** 08b2b8b
    - [x] Write unit tests to verify `AppSettings` serialization.
    - [x] Verify `StartupService` correctly manages the file in the startup directory.
- [x] **Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)** 6aa5c14

## Phase 2: Main Window Visual Modernization [checkpoint: d88b94c]
- [x] **Task: Implement Dynamic Background Logic** 3de0c3c
    - [x] Add `CurrentBackgroundColor` property to `MainViewModel`.
    - [x] Implement color conversion logic: darken the user-set Work/Break colors.
- [x] **Task: Progress Bar Hover Effect (XAML Style)** e9fca87
    - [x] Update `MainWindow.xaml` to add a `Style` and `ControlTemplate` for the progress bar.
    - [x] Use `Trigger` for `IsMouseOver` to change the foreground or opacity.
- [x] **Task: Icon-based Skip Button** 2f4aab1
    - [x] Use `Path` to draw a ⏩ icon instead of text.
    - [x] Apply modern button style (borderless, hover highlight).
- [x] **Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)** d88b94c

## Phase 3: Settings Window Enhancement (GUI Pickers) [checkpoint: e97fa2e]
- [x] **Task: Audio File Picker**
    - [x] Add "Browse" button in the settings window linked to `Microsoft.Win32.OpenFileDialog`.
- [x] **Task: Font Family Picker**
    - [x] Populate a ComboBox with `System.Windows.Media.Fonts.SystemFontFamilies`.
- [x] **Task: Native Color Picker Integration**
    - [x] Implement a mechanism to open a color picker (custom XAML preset color picker) when clicking color preview areas.
- [x] **Task: Integration Test for Settings UI**
    - [x] Ensure UI values correctly write back to `AppSettings` and persist to disk.
- [x] **Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)** e97fa2e