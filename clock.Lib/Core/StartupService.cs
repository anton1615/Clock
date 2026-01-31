using System;
using System.Diagnostics;
using System.IO;

namespace clock.Core
{
    public static class StartupService
    {
        private static readonly string StartupFolderPath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            @"Microsoft\Windows\Start Menu\Programs\Startup"
        );
        
        private static readonly string ShortcutPath = Path.Combine(StartupFolderPath, "Clock.lnk");

        public static void SetStartup(bool enable)
        {
            try
            {
                if (enable)
                {
                    if (File.Exists(ShortcutPath)) return;

                    string targetPath = Environment.ProcessPath ?? string.Empty;
                    if (string.IsNullOrEmpty(targetPath)) return;

                    // Escape single quotes for PowerShell
                    string escapedPath = targetPath.Replace("'", "''");

                    // Ensure directory exists (should exist on standard Windows)
                    if (!Directory.Exists(StartupFolderPath))
                    {
                        Directory.CreateDirectory(StartupFolderPath);
                    }

                    string script = "$WshShell = New-Object -ComObject WScript.Shell; " + 
                                   "$Shortcut = $WshShell.CreateShortcut('" + ShortcutPath + "'); " + 
                                   "$Shortcut.TargetPath = '" + escapedPath + "'; " + 
                                   "$Shortcut.Save()";

                    Process.Start(new ProcessStartInfo
                    {
                        FileName = "powershell",
                        Arguments = $"-NoProfile -Command \"{script}\"",
                        CreateNoWindow = true,
                        WindowStyle = ProcessWindowStyle.Hidden
                    })?.WaitForExit();
                }
                else
                {
                    if (File.Exists(ShortcutPath))
                    {
                        File.Delete(ShortcutPath);
                    }
                }
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"[StartupService Error] {ex.Message}");
            }
        }
        
        public static bool IsStartupEnabled()
        {
            return File.Exists(ShortcutPath);
        }
    }
}
