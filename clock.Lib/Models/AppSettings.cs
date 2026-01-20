using CommunityToolkit.Mvvm.ComponentModel;
using System;
using System.IO;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Windows;
using System.Windows.Media;

namespace clock.Models
{
    public partial class AppSettings : ObservableObject
    {
        [ObservableProperty] private int _workDuration = 30;
        [ObservableProperty] private int _breakDuration = 6;
        
        [ObservableProperty] 
        [NotifyPropertyChangedFor(nameof(BackgroundColorBrush))]
        private byte _backgroundAlpha = 200;

        [ObservableProperty] private double _fontSize = 50;
        [ObservableProperty] private string _fontFamily = "Segoe UI";
        
        [ObservableProperty] 
        [NotifyPropertyChangedFor(nameof(TextColorBrush))]
        private string _textColor = "White";
        
        [ObservableProperty] 
        [NotifyPropertyChangedFor(nameof(SelectedFontWeight))]
        private bool _isBold = true;

        [ObservableProperty] private double _volume = 50;
        [ObservableProperty] private string _workColor = "#FF8C00";
        [ObservableProperty] private string _breakColor = "#32CD32";
        [ObservableProperty] private string _soundPath = "Assets/notify.wav";
        [ObservableProperty] private bool _isStartupEnabled = false;

        [JsonIgnore]
        public FontWeight SelectedFontWeight => IsBold ? FontWeights.Bold : FontWeights.Normal;

        [JsonIgnore]
        public SolidColorBrush BackgroundColorBrush => new SolidColorBrush(Color.FromArgb(BackgroundAlpha, 0, 0, 0));

        [JsonIgnore]
        public SolidColorBrush TextColorBrush
        {
            get
            {
                try
                {
                    var converter = new BrushConverter();
                    var brush = converter.ConvertFromString(TextColor ?? "White") as SolidColorBrush;
                    return brush ?? Brushes.White;
                }
                catch
                {
                    return Brushes.White;
                }
            }
        }

        private static readonly string _filePath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "setting.json");

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
            if (settings.FontSize < 10 || settings.FontSize > 200)
            {
                settings.FontSize = 50;
            }

            return settings;
        }
    }
}