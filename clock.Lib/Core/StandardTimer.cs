using System;
using System.Timers;

namespace clock.Core
{
    /// <summary>
    /// 使用 System.Timers.Timer 實作的通用計時器，適用於非 UI 執行緒與跨平台環境。
    /// </summary>
    public class StandardTimer : ITimer
    {
        private readonly System.Timers.Timer _timer;

        public event EventHandler? Tick;

        public TimeSpan Interval
        {
            get => TimeSpan.FromMilliseconds(_timer.Interval);
            set => _timer.Interval = value.TotalMilliseconds;
        }

        public StandardTimer()
        {
            _timer = new System.Timers.Timer();
            _timer.Elapsed += (s, e) => Tick?.Invoke(this, EventArgs.Empty);
        }

        public void Start() => _timer.Start();
        public void Stop() => _timer.Stop();
    }
}