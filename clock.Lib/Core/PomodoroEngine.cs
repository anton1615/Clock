using System;
using CommunityToolkit.Mvvm.ComponentModel;
using clock.Models;

namespace clock.Core
{
    /// <summary>
    /// 番茄鐘核心引擎，負責計時邏輯與階段切換。
    /// </summary>
    public partial class PomodoroEngine : ObservableObject
    {
        private readonly AppSettings _settings;
        private readonly ITimer _timer;

        [ObservableProperty] private TimeSpan _timeRemaining;
        [ObservableProperty] private bool _isWorkPhase;
        [ObservableProperty] private string _currentPhaseName = "";
        [ObservableProperty] private bool _isPaused;

        private TimeSpan _totalDuration;
        /// <summary>
        /// 當前階段的總時長。
        /// </summary>
        public TimeSpan TotalDuration => _totalDuration;

        /// <summary>
        /// 當階段計時完成時觸發。
        /// </summary>
        public event Action? OnPhaseCompleted;

        /// <summary>
        /// 建構番茄鐘引擎。
        /// </summary>
        /// <param name="settings">應用程式設定。</param>
        /// <param name="timer">選用的計時器實作。若為 null 則預設使用 StandardTimer。</param>
        public PomodoroEngine(AppSettings settings, ITimer? timer = null)
        {
            _settings = settings;
            _isWorkPhase = true;
            
            _timer = timer ?? new StandardTimer();
            _timer.Interval = TimeSpan.FromSeconds(1);
            _timer.Tick += Timer_Tick;
            
            StartPhase();
        }

        private void StartPhase()
        {
            int minutes = IsWorkPhase ? _settings.WorkDuration : _settings.BreakDuration;
            
            // 安全防呆：如果設定值異常，強制使用預設值
            if (minutes <= 0) minutes = IsWorkPhase ? 30 : 5;

            _totalDuration = TimeSpan.FromMinutes(minutes);
            TimeRemaining = _totalDuration;
            
            CurrentPhaseName = IsWorkPhase ? "WORK" : "BREAK";
            IsPaused = false;
            _timer.Start();
        }

        private void Timer_Tick(object? sender, EventArgs e)
        {
            if (IsPaused) return;

            if (TimeRemaining.TotalSeconds > 0)
            {
                TimeRemaining = TimeRemaining.Subtract(TimeSpan.FromSeconds(1));
            }
            else
            {
                OnPhaseCompleted?.Invoke();
                TogglePhase();
            }
        }

        /// <summary>
        /// 手動切換當前階段 (Work <-> Break)。
        /// </summary>
        public void TogglePhase()
        {
            IsWorkPhase = !IsWorkPhase;
            StartPhase();
            IsPaused = false; 
        }

        /// <summary>
        /// 切換暫停/繼續狀態。
        /// </summary>
        public void TogglePause()
        {
            IsPaused = !IsPaused;
        }
    }
}