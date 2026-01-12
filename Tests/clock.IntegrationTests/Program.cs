using System;
using clock.Core;
using clock.Models;

namespace clock.IntegrationTests
{
    class Program
    {
        [STAThread]
        static void Main(string[] args)
        {
            Console.WriteLine("Running PomodoroEngine Tests...");

            try
            {
                Test_InitialState();
                Test_TogglePhase();
                Test_TogglePause();
                Test_Settings();
                Console.WriteLine("All tests passed!");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Tests failed: {ex.Message}");
                Console.WriteLine(ex.StackTrace);
                Environment.Exit(1);
            }
        }

        static void Test_Settings()
        {
            var settings = new AppSettings { WorkDuration = 99 };
            settings.Save();

            var loaded = AppSettings.Load();
            if (loaded.WorkDuration != 99) throw new Exception("Settings persistence failed");
            
            Console.WriteLine("Test_Settings Passed");
        }

        static void Test_InitialState()
        {
            var settings = new AppSettings { WorkDuration = 25, BreakDuration = 5 };
            // PomodoroEngine creates a DispatcherTimer.
            // DispatcherTimer requires a Dispatcher on the current thread, or it might fail/do nothing.
            // In a console app, we don't have a message loop by default.
            // However, object creation might succeed.
            var engine = new PomodoroEngine(settings);

            if (!engine.IsWorkPhase) throw new Exception("Initial state should be WorkPhase");
            if (engine.TimeRemaining != TimeSpan.FromMinutes(25)) throw new Exception("Initial time incorrect");
            if (engine.IsPaused) throw new Exception("Initial state should not be paused");
            
            Console.WriteLine("Test_InitialState Passed");
        }

        static void Test_TogglePhase()
        {
            var settings = new AppSettings { WorkDuration = 25, BreakDuration = 5 };
            var engine = new PomodoroEngine(settings);

            engine.TogglePhase();

            if (engine.IsWorkPhase) throw new Exception("TogglePhase failed to switch phase");
            if (engine.TimeRemaining != TimeSpan.FromMinutes(5)) throw new Exception("TogglePhase time incorrect");
            
            Console.WriteLine("Test_TogglePhase Passed");
        }

        static void Test_TogglePause()
        {
            var settings = new AppSettings { WorkDuration = 25, BreakDuration = 5 };
            var engine = new PomodoroEngine(settings);

            engine.TogglePause();
            if (!engine.IsPaused) throw new Exception("TogglePause failed to pause");

            engine.TogglePause();
            if (engine.IsPaused) throw new Exception("TogglePause failed to unpause");
            
            Console.WriteLine("Test_TogglePause Passed");
        }
    }
}