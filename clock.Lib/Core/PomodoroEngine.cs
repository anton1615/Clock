using System;
using CommunityToolkit.Mvvm.ComponentModel;
using clock.Models;

namespace clock.Core
{
    public partial class PomodoroEngine : ObservableObject
    {
        private readonly AppSettings _settings;
        private readonly ITimer _timer;

        [ObservableProperty] private TimeSpan _timeRemaining;
        [ObservableProperty] private bool _isWorkPhase;
        [ObservableProperty] private string _currentPhaseName = "";
        [ObservableProperty] private bool _isPaused;

        private TimeSpan _totalDuration;
        public TimeSpan TotalDuration => _totalDuration;

        // 核心：絕對目標結束時間 (UTC)
        private DateTime _targetEndTime; 

        public event Action? OnPhaseCompleted;
        public event Action? OnSignificantStateChanged;

        public PomodoroEngine(AppSettings settings, ITimer? timer = null)
        {
            _settings = settings;
            _isWorkPhase = true;
            _isPaused = false; // 改回：啟動即自動開始
            _timer = timer ?? new StandardTimer();
            _timer.Interval = TimeSpan.FromMilliseconds(100); 
            _timer.Tick += Timer_Tick;
            
            StartPhase(false); 
        }

        private void StartPhase(bool startPaused = false)
        {
            int minutes = IsWorkPhase ? _settings.WorkDuration : _settings.BreakDuration;
            if (minutes <= 0) minutes = IsWorkPhase ? 25 : 5;
            _totalDuration = TimeSpan.FromMinutes(minutes);
            TimeRemaining = _totalDuration;
            CurrentPhaseName = IsWorkPhase ? "WORK" : "BREAK";
            
            IsPaused = startPaused;
            if (!IsPaused) {
                // 設定目標點 = 現在 + 剩餘時間
                _targetEndTime = DateTime.UtcNow.Add(TimeRemaining);
                _timer.Start();
            } else {
                _timer.Stop();
            }
            OnSignificantStateChanged?.Invoke();
        }

        private void Timer_Tick(object? sender, EventArgs e)
        {
            if (IsPaused) return;

            // 【高精度校準】始終以物理 UTC 時間為準計算
            var now = DateTime.UtcNow;
            var remaining = _targetEndTime - now;
            
            if (remaining <= TimeSpan.Zero) {
                TimeRemaining = TimeSpan.Zero;
                _timer.Stop();
                OnPhaseCompleted?.Invoke();
                TogglePhase();
            } else {
                TimeRemaining = remaining;
            }
        }

        public void TogglePhase() { 
            IsWorkPhase = !IsWorkPhase; 
            StartPhase(false); // 切換階段後自動開始
        }
        
        public void TogglePause() { 
            IsPaused = !IsPaused;
            if (!IsPaused) {
                // 繼續時重新設定目標
                _targetEndTime = DateTime.UtcNow.Add(TimeRemaining);
                _timer.Start();
            } else {
                _timer.Stop();
            }
            OnSignificantStateChanged?.Invoke();
        }

        public EngineState GetState()
        {
            long targetEndTimeUnix = 0;
            if (!IsPaused) targetEndTimeUnix = new DateTimeOffset(_targetEndTime).ToUnixTimeMilliseconds();
            
            return new EngineState {
                RemainingSeconds = TimeRemaining.TotalSeconds,
                IsWorkPhase = IsWorkPhase,
                IsPaused = IsPaused,
                PhaseName = CurrentPhaseName,
                TotalDurationSeconds = TotalDuration.TotalSeconds,
                TargetEndTimeUnix = targetEndTimeUnix
            };
        }

        public void ApplyState(EngineState state)
        {
            IsWorkPhase = state.IsWorkPhase;
            IsPaused = state.IsPaused;
            CurrentPhaseName = state.PhaseName;
            _totalDuration = TimeSpan.FromSeconds(state.TotalDurationSeconds);
            
            if (!state.IsPaused && state.TargetEndTimeUnix > 0) {
                _targetEndTime = DateTimeOffset.FromUnixTimeMilliseconds(state.TargetEndTimeUnix).UtcDateTime;
                var diff = _targetEndTime - DateTime.UtcNow;
                TimeRemaining = diff > TimeSpan.Zero ? diff : TimeSpan.Zero;
            } else {
                TimeRemaining = TimeSpan.FromSeconds(state.RemainingSeconds);
            }

            if (IsPaused) _timer.Stop(); else _timer.Start();
        }
    }
}