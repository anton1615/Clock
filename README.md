# Clock - Minimalist Pomodoro Timer

A minimalist, unobtrusive Pomodoro timer widget for Windows, built with .NET 10 and WPF.
Designed to stay out of your way while keeping you focused.

![License](https://img.shields.io/badge/license-MIT-blue.svg) ![Platform](https://img.shields.io/badge/platform-Windows-lightgrey.svg)

## Features

*   **Minimalist Widget**: Frameless window that sits quietly on your desktop.
*   **Always Visible**: Smart positioning logic ensures it stays on top of your taskbar but doesn't block your work.
*   **Two-Phase Cycle**: Simple "WORK" and "BREAK" phases with visual progress indicators.
*   **Instant Interaction**: Click to pause/resume, one-click phase switching.
*   **Hacker-Friendly Config**: Customize colors, fonts, and behavior via `setting.json`.
*   **System Tray Integration**: Hides gracefully to the tray when needed.

## Installation & Setup

1.  Download the latest release (or build from source).
2.  Run `clock.exe`.

### 🎵 Important: Sound Effects Setup

To respect copyright laws, **this repository does not include sound files**. You need to provide your own notification sound.

1.  Find a sound effect you like (e.g., a `.wav` file).
2.  Rename it to `notify.wav` (optional, but recommended default).
3.  Place the file in the `Assets` folder next to the executable.
    *   *Alternatively*, you can specify a custom path in `setting.json`.

## Configuration (`setting.json`)

On the first run, a `setting.json` file will be generated in the application directory. You can edit this file to customize your experience:

```json
{
  "WorkDuration": 30,          // Minutes
  "BreakDuration": 6,          // Minutes
  "BackgroundAlpha": 200,      // 0-255 (Transparency)
  "FontSize": 50,              // Widget size scales with font size
  "FontFamily": "Segoe UI",
  "TextColor": "White",
  "IsBold": true,
  "Volume": 50,                // 0-100
  "WorkColor": "#FF8C00",      // Hex color
  "BreakColor": "#32CD32",
  "SoundPath": "Assets/notify.wav" // Path to your sound file
}
```

## Development

### Requirements
*   .NET 10 SDK
*   Visual Studio 2022 (or later) / VS Code

### Build
```bash
dotnet build
```

### Architecture
This project follows a strict **MVVM** architecture with **Dependency Injection**:
*   `clock`: The WPF executable (Views, Composition Root).
*   `clock.Lib`: Core logic, Models, and ViewModels (UI-agnostic).
*   `Tests`: Integration tests ensuring core stability.

## License

This project is licensed under the [MIT License](LICENSE).
App icon is AI-generated and free to use.
