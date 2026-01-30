using System;

namespace clock.Core
{
    /// <summary>
    /// 定義計時器的通用介面，以便在不同平台（WPF, Android）或測試環境中使用。
    /// </summary>
    public interface ITimer
    {
        /// <summary>
        /// 當計時器間隔到達時觸發。
        /// </summary>
        event EventHandler Tick;

        /// <summary>
        /// 計時器間隔。
        /// </summary>
        TimeSpan Interval { get; set; }

        /// <summary>
        /// 開始計時。
        /// </summary>
        void Start();

        /// <summary>
        /// 停止計時。
        /// </summary>
        void Stop();
    }
}