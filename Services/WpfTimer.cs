using System;
using System.Windows.Threading;
using clock.Core;

namespace clock.Services
{
    /// <summary>
    /// 使用 WPF 的 DispatcherTimer 實作 ITimer，確保 Tick 事件在 UI 執行緒上觸發。
    /// </summary>
    public class WpfTimer : clock.Core.ITimer
    {
        private readonly DispatcherTimer _timer;

        public event EventHandler? Tick;

        public TimeSpan Interval
        {
            get => _timer.Interval;
            set => _timer.Interval = value;
        }

        public WpfTimer()
        {
            _timer = new DispatcherTimer();
            _timer.Tick += (s, e) => Tick?.Invoke(this, EventArgs.Empty);
        }

        public void Start() => _timer.Start();
        public void Stop() => _timer.Stop();
    }
}