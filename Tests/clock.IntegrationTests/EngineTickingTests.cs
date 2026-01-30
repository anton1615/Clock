using System;
using System.Threading;
using clock.Core;
using clock.Models;

namespace clock.IntegrationTests
{
    public class EngineTickingTests
    {
        public static void RunTests()
        {
            Test_EngineCountsDown();
        }

        static void Test_EngineCountsDown()
        {
            var settings = new AppSettings { WorkDuration = 25 };
            // Use StandardTimer which works in console apps
            var timer = new StandardTimer();
            var engine = new PomodoroEngine(settings, timer);
            
            var initialTime = engine.TimeRemaining;
            
            // Wait for 1.5 seconds to ensure at least one tick (1s interval)
            Thread.Sleep(1500); 

            if (engine.TimeRemaining >= initialTime)
            {
                throw new Exception($"Engine failed to count down. Initial: {initialTime}, Current: {engine.TimeRemaining}");
            }
            
            Console.WriteLine("Test_EngineCountsDown Passed");
        }
    }
}