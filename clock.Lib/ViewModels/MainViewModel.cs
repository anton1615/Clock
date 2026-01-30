using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using clock.Core;
using clock.Models;
using System.Windows;
using System;
using System.Windows.Media;
using System.IO;

namespace clock.ViewModels
{
    /// <summary>
    /// 主視窗的 ViewModel，負責處理 UI 綁定、命令與視窗控制。
    /// </summary>
    public partial class MainViewModel : ObservableObject
    {
        /// <summary>
        /// 當前應用程式設定。
        /// </summary>
        public AppSettings Settings { get; }
        private readonly PomodoroEngine _engine;
        private readonly ISettingsService _settingsService;

        /// <summary>
        /// 番茄鐘引擎實例。
        /// </summary>
        public PomodoroEngine Engine => _engine;
        
        [ObservableProperty] private string _displayTime = "00:00";
        [ObservableProperty] private double _windowWidth;
        [ObservableProperty] private double _windowHeight;

        /// <summary>
        /// 進度條數值 (0.0 ~ 1.0)。
        /// </summary>
        public double ProgressValue 
        {
            get 
            {
                if (_engine.TotalDuration.TotalSeconds <= 0) return 1.0;
                return _engine.TimeRemaining.TotalSeconds / _engine.TotalDuration.TotalSeconds;
            }
        }
        
        /// <summary>
        /// 根據當前階段回傳對應的畫筆顏色。
        /// </summary>
        public SolidColorBrush CurrentPhaseColor
        {
            get
            {
                string colorHex = _engine.IsWorkPhase ? Settings.WorkColor : Settings.BreakColor;
                try
                {
                    var brush = new BrushConverter().ConvertFromString(colorHex ?? "#FF8C00") as SolidColorBrush;
                    return brush ?? Brushes.Orange;
                }
                catch { return Brushes.Orange; }
            }
        }

        public MainViewModel(ISettingsService settingsService, clock.Core.ITimer? timer = null)
        {
            _settingsService = settingsService;
            Settings = AppSettings.Load();
            
            if (Settings.WindowSize < 10 || Settings.WindowSize > 200) 
            {
                Settings.WindowSize = 50; 
            }

            UpdateWindowSize();

            _engine = new PomodoroEngine(Settings, timer);
            _engine.OnPhaseCompleted += Engine_OnPhaseCompleted;
            UpdateDisplayTime();

            _engine.PropertyChanged += (s, e) =>
            {
                if (e.PropertyName == nameof(PomodoroEngine.TimeRemaining))
                {
                    UpdateDisplayTime();
                    OnPropertyChanged(nameof(ProgressValue));
                }
                if (e.PropertyName == nameof(PomodoroEngine.IsWorkPhase))
                {
                    OnPropertyChanged(nameof(CurrentPhaseColor));
                    OnPropertyChanged(nameof(CurrentBackgroundColor));
                }
                if (e.PropertyName == nameof(PomodoroEngine.IsPaused)) OnPropertyChanged(nameof(Engine)); 
            };

            Settings.PropertyChanged += (s, e) =>
            {
                if (e.PropertyName == nameof(AppSettings.WindowSize))
                {
                    UpdateWindowSize();
                }
                if (e.PropertyName == nameof(AppSettings.IsBold)) OnPropertyChanged(nameof(Settings));
                if (e.PropertyName == nameof(AppSettings.WorkColor) || e.PropertyName == nameof(AppSettings.BreakColor) || e.PropertyName == nameof(AppSettings.BackgroundAlpha))
                {
                    OnPropertyChanged(nameof(CurrentPhaseColor));
                    OnPropertyChanged(nameof(CurrentBackgroundColor));
                }
            };
        }

        /// <summary>
        /// 根據當前階段回傳對應的深色背景。
        /// </summary>
        public SolidColorBrush CurrentBackgroundColor
        {
            get
            {
                var baseColor = CurrentPhaseColor.Color;
                // 將顏色變深：乘以 0.3 左右 (比之前亮一點)
                var darkColor = Color.FromArgb(
                    Settings.BackgroundAlpha,
                    (byte)(baseColor.R * 0.3),
                    (byte)(baseColor.G * 0.3),
                    (byte)(baseColor.B * 0.3)
                );
                return new SolidColorBrush(darkColor);
            }
        }

        private void UpdateWindowSize()
        {
            double width = Settings.WindowSize * 6.0;
            if (width < 60) width = 60;
            if (width > 1200) width = 1200;

            WindowWidth = width;
            WindowHeight = width * 0.38; // 從 0.45 調為 0.38，更貼近 Border 的比例 (125/350 ≈ 0.36)
        }

        private void UpdateDisplayTime()
        {
            DisplayTime = _engine.TimeRemaining.ToString(@"mm\:ss");
        }

        private void Engine_OnPhaseCompleted()
        {
            try
            {
                string soundPath = Settings.SoundPath;
                if (!Path.IsPathRooted(soundPath))
                {
                    soundPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, soundPath);
                }
                
                if (File.Exists(soundPath))
                {
                    // 使用新的 AudioHelper，保證最大音量且不怕休眠
                    double vol = Settings.Volume / 100.0;
                    AudioHelper.PlaySound(soundPath, vol);
                }
                else
                {
                    System.Media.SystemSounds.Exclamation.Play();
                }
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"Playback failed: {ex.Message}");
            }
        }

        [RelayCommand] private void TogglePhase() => _engine.TogglePhase();
        [RelayCommand] private void TogglePause() => _engine.TogglePause();
        [RelayCommand] private void Exit() => Application.Current.Shutdown();

        [RelayCommand]
        private void ToggleWidgetVisibility()
        {
            var window = Application.Current.MainWindow;
            if (window != null)
            {
                if (window.Visibility == Visibility.Visible) window.Hide();
                else { window.Show(); window.Activate(); }
            }
        }

        [RelayCommand]
        private void OpenSettings()
        {
            _settingsService.OpenSettings(Settings);
        }
    }
}