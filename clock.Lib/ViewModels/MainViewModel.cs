using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using clock.Core;
using clock.Models;
using System;
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
        private readonly IAudioService _audioService;

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

        public MainViewModel(ISettingsService settingsService, clock.Core.ITimer? timer = null, IAudioService? audioService = null)
        {
            _settingsService = settingsService;
            _audioService = audioService ?? new NullAudioService(); // 預設使用不播放聲音的服務
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
                    // 通知 UI 更新相關顏色（透過 Converter）
                    OnPropertyChanged(nameof(Engine));
                }
                if (e.PropertyName == nameof(PomodoroEngine.IsPaused)) OnPropertyChanged(nameof(Engine)); 
            };

            Settings.PropertyChanged += (s, e) =>
            {
                if (e.PropertyName == nameof(AppSettings.WindowSize))
                {
                    UpdateWindowSize();
                }
                // 當設定變更時，通知 UI 重新評估綁定
                OnPropertyChanged(nameof(Settings));
            };
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
                
                double vol = Settings.Volume / 100.0;
                _audioService.Play(soundPath, vol);
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"Playback trigger failed: {ex.Message}");
            }
        }

        [RelayCommand] private void TogglePhase() => _engine.TogglePhase();
        [RelayCommand] private void TogglePause() => _engine.TogglePause();
        
        // 注意：Exit 依賴於 Application.Current.Shutdown()，這是 WPF 專有的
        // 我們應該透過一個 IUIService 來處理
        [RelayCommand] private void Exit() 
        {
            // 暫時留空或在 WPF 專案中處理
        }

        [RelayCommand]
        private void ToggleWidgetVisibility()
        {
            // 同樣依賴 WPF 的視窗操作，應由 View 層處理
        }

        [RelayCommand]
        private void OpenSettings()
        {
            _settingsService.OpenSettings(Settings);
        }
    }
}