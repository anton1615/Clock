using clock.Models;
using System;
using System.IO;

namespace clock.IntegrationTests
{
    public class SettingsTests
    {
        public static void RunTests()
        {
            Test_AppSettings_SaveAndLoadShowConsole();
        }

        static void Test_AppSettings_SaveAndLoadShowConsole()
        {
            // Arrange
            var settings = new AppSettings();
            settings.ShowConsole = false; 

            // Act
            settings.Save();
            var loaded = AppSettings.Load();

            // Assert
            if (loaded.ShowConsole != false)
            {
                throw new Exception("AppSettings failed to save/load ShowConsole property.");
            }
            
            // Cleanup: Reset
            loaded.ShowConsole = true;
            loaded.Save();
            
            Console.WriteLine("Test_AppSettings_SaveAndLoadShowConsole Passed");
        }
    }
}