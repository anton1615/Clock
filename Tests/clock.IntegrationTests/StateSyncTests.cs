using System;
using clock.Core;
using clock.Models;

namespace clock.IntegrationTests
{
    public class StateSyncTests
    {
        public static void RunTests()
        {
            Test_StateSync();
        }

        static void Test_StateSync()
        {
            var settings = new AppSettings { WorkDuration = 25, BreakDuration = 5 };
            var engine = new PomodoroEngine(settings);
            
            // 模擬一些狀態變更
            engine.TogglePhase(); // 變成 BREAK 階段 (IsPaused = false)
            engine.TogglePause(); // 變成暫停 (IsPaused = true)
            
            var state = engine.GetState();
            
            if (state.IsPaused != true) throw new Exception($"State sync: IsPaused mismatch. Expected true, got {state.IsPaused}");
            if (state.IsWorkPhase != false) throw new Exception("State sync: IsWorkPhase mismatch");
            if (state.PhaseName != "BREAK") throw new Exception("State sync: PhaseName mismatch");
            
            // 建立一個新的引擎並套用狀態
            var newEngine = new PomodoroEngine(settings);
            newEngine.ApplyState(state);
            
            if (newEngine.IsPaused != true) throw new Exception("Apply state: IsPaused mismatch");
            if (newEngine.IsWorkPhase != false) throw new Exception("Apply state: IsWorkPhase mismatch");
            if (newEngine.CurrentPhaseName != "BREAK") throw new Exception("Apply state: CurrentPhaseName mismatch");
            if (Math.Abs(newEngine.TimeRemaining.TotalSeconds - state.RemainingSeconds) > 0.1) 
                throw new Exception("Apply state: TimeRemaining mismatch");

            Console.WriteLine("Test_StateSync Passed");
        }
    }
}