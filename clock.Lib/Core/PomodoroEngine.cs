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

        /// <summary>
        /// 獲取當前引擎狀態快照。
        /// </summary>
        public EngineState GetState()
        {
            var now = DateTimeOffset.UtcNow;
            long targetEndTime = 0;

            if (!IsPaused)
            {
                targetEndTime = now.Add(TimeRemaining).ToUnixTimeMilliseconds();
            }

            return new EngineState
            {
                RemainingSeconds = TimeRemaining.TotalSeconds,
                IsWorkPhase = IsWorkPhase,
                IsPaused = IsPaused,
                PhaseName = CurrentPhaseName,
                TotalDurationSeconds = TotalDuration.TotalSeconds,
                TargetEndTimeUnix = targetEndTime
            };
        }

        /// <summary>
        /// 從外部狀態更新引擎。
        /// </summary>
        /// <param name="state">新的引擎狀態。</param>
        public void ApplyState(EngineState state)
        {
            IsWorkPhase = state.IsWorkPhase;
            IsPaused = state.IsPaused;
            CurrentPhaseName = state.PhaseName;
            _totalDuration = TimeSpan.FromSeconds(state.TotalDurationSeconds);
            
            // 如果 TargetEndTimeUnix 有值且未暫停，嘗試校正時間
            if (!state.IsPaused && state.TargetEndTimeUnix > 0)
            {
                var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
                var diff = state.TargetEndTimeUnix - now;
                TimeRemaining = diff > 0 ? TimeSpan.FromMilliseconds(diff) : TimeSpan.Zero;
            }
            else
            {
                TimeRemaining = TimeSpan.FromSeconds(state.RemainingSeconds);
            }

            if (IsPaused) _timer.Stop();
            else _timer.Start();
        }
    }
}