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
            Test_AppSettings_SaveAndLoadPauseSettings();
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

        static void Test_AppSettings_SaveAndLoadPauseSettings()
        {
            // Arrange - verify defaults
            var settings = new AppSettings();
            if (settings.PausedColor != "#1E90FF")
            {
                throw new Exception($"AppSettings default PausedColor should be #1E90FF but was {settings.PausedColor}.");
            }
            if (settings.IsPauseBlinkEnabled != false)
            {
                throw new Exception("AppSettings default IsPauseBlinkEnabled should be false.");
            }

            // Act
            settings.PausedColor = "#1E90FF";
            settings.IsPauseBlinkEnabled = true;
            settings.Save();
            var loaded = AppSettings.Load();

            // Assert
            if (loaded.PausedColor != "#1E90FF")
            {
                throw new Exception("AppSettings failed to save/load PausedColor property.");
            }
            if (loaded.IsPauseBlinkEnabled != true)
            {
                throw new Exception("AppSettings failed to save/load IsPauseBlinkEnabled property.");
            }

            // Cleanup: Reset
            loaded.IsPauseBlinkEnabled = false;
            loaded.Save();

            Console.WriteLine("Test_AppSettings_SaveAndLoadPauseSettings Passed");
        }
    }
}