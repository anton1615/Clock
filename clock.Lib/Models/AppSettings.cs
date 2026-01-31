using CommunityToolkit.Mvvm.ComponentModel;
using System;
using System.IO;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace clock.Models
{
    public partial class AppSettings : ObservableObject
    {
        [ObservableProperty] private int _workDuration = 25;
        [ObservableProperty] private int _breakDuration = 5;
        
        [ObservableProperty] private byte _backgroundAlpha = 200;

        [ObservableProperty] private double _windowSize = 50;
        [ObservableProperty] private string _fontFamily = "Segoe UI";
        
        [ObservableProperty] private string _textColor = "White";
        
        [ObservableProperty] private bool _isBold = true;

        [ObservableProperty] private double _volume = 50;
        [ObservableProperty] private string _workColor = "#FF8C00";
        [ObservableProperty] private string _breakColor = "#32CD32";
        [ObservableProperty] private string _soundPath = "Assets/notify.wav";
        [ObservableProperty] private bool _isStartupEnabled = false;
        [ObservableProperty] private bool _showConsole = true;

        private static readonly string _oldFilePath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "setting.json");
        private static readonly string _filePath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "Clock",
            "setting.json"
        );

        public void Save()
        {
            try
            {
                string? dir = Path.GetDirectoryName(_filePath);
                if (dir != null && !Directory.Exists(dir)) Directory.CreateDirectory(dir);
                
                var options = new JsonSerializerOptions { WriteIndented = true };
                File.WriteAllText(_filePath, JsonSerializer.Serialize(this, options));
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[Settings Error] Save failed: {ex.Message}");
            }
        }

        public static AppSettings Load()
        {
            // 遷移邏輯：如果新位置沒有檔案但舊位置有，則搬移
            try
            {
                if (!File.Exists(_filePath) && File.Exists(_oldFilePath))
                {
                    string? dir = Path.GetDirectoryName(_filePath);
                    if (dir != null && !Directory.Exists(dir)) Directory.CreateDirectory(dir);
                    File.Move(_oldFilePath, _filePath);
                    Console.WriteLine("[Settings] Migrated settings to LocalApplicationData.");
                }
            }
            catch { /* Ignore migration errors */ }

            AppSettings settings;
            try
            {
                if (!File.Exists(_filePath)) 
                {
                    return new AppSettings();
                }
                settings = JsonSerializer.Deserialize<AppSettings>(File.ReadAllText(_filePath)) ?? new AppSettings();
            }
            catch
            {
                settings = new AppSettings();
            }

            // 修正：將下限從 20 改為 10，避免使用者設很小時被重置
            if (settings.WindowSize < 10 || settings.WindowSize > 200)
            {
                settings.WindowSize = 50;
            }

            return settings;
        }
    }
}