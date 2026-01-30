using System;

namespace clock.Models
{
    /// <summary>
    /// 番茄鐘引擎的狀態資料傳輸物件 (DTO)，用於即時同步。
    /// </summary>
    public class EngineState
    {
        /// <summary>
        /// 剩餘秒數。
        /// </summary>
        public double RemainingSeconds { get; set; }

        /// <summary>
        /// 是否為工作階段。
        /// </summary>
        public bool IsWorkPhase { get; set; }

        /// <summary>
        /// 是否暫停。
        /// </summary>
        public bool IsPaused { get; set; }

        /// <summary>
        /// 階段名稱 (WORK/BREAK)。
        /// </summary>
        public string PhaseName { get; set; } = string.Empty;

        /// <summary>
        /// 當前階段總時長（秒）。
        /// </summary>
        public double TotalDurationSeconds { get; set; }

        /// <summary>
        /// 預計結束的時間點（UTC 毫秒時間戳）。
        /// 用於校正網路延遲，手機端可根據此值重新計算倒數。
        /// </summary>
        public long TargetEndTimeUnix { get; set; }
    }
}