# Track Specification: UI/UX Modernization & Settings Extension

## 1. Overview
This track aims to comprehensively enhance the visual quality and user experience of the Clock Widget. It will replace the static black background with dynamic dark tones linked to the current phase, modernize UI controls with hover effects, and provide intuitive GUI pickers for all settings, including a new "Run at Startup" option.

## 2. Functional Requirements
- **Dynamic Theme Background**:
    - Remove the solid black background. Background color must change based on the phase.
    - Background color should be a very dark version of the respective phase's bar color to maintain text readability.
- **Interactive Progress Bar**:
    - **MouseOver Feedback**: When the mouse hovers over the progress bar, its color must lighten (highlight), indicating to the user that it is a clickable area (pause/play).
- **Modernized Controls (Native-based Custom Styles)**:
    - Use custom XAML Templates to optimize buttons while maintaining a lightweight footprint (no heavy UI frameworks).
    - **Skip Phase Button**: Change to an icon-based design (⏩) with MouseOver highlight support.
- **Enhanced Settings Window**:
    - **Audio Path**: Add a "Browse" button using a system file dialog to select `.wav` files.
    - **Run at Startup**: Add a Checkbox to create/remove a shortcut in `%AppData%\Microsoft\Windows\Start Menu\Programs\Startup`.
    - **Color Pickers**: Use GUI color pickers for Text, Work Bar, and Break Bar colors.
    - **Font Selection**: Add a ComboBox populated with system font families.

## 3. Non-Functional Requirements
- **Maintain Lightweight Footprint**: Ensure the single-file executable size does not increase significantly.
- **Backward Compatibility**: Ensure new `setting.json` fields have defaults so old config files don't cause crashes.

## 4. Acceptance Criteria
- [ ] Background color updates correctly based on the current phase.
- [ ] Progress bar color lightens on mouse hover.
- [ ] Settings window allows picking colors, fonts, and audio paths via GUI.
- [ ] "Run at Startup" correctly creates/removes a shortcut in the startup folder.
- [ ] Skip button is icon-based and functional.

## 5. Out of Scope
- Complex animations (e.g., window fade in/out).
- Custom image backgrounds.
