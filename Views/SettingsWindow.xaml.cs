using System;
using System.Linq;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using Microsoft.Win32;
using clock.Models;

namespace clock.Views
{
    public partial class SettingsWindow : Window
    {
        private AppSettings _settings;

        public SettingsWindow(AppSettings settings)
        {
            InitializeComponent();
            _settings = settings;

            WorkDurationBox.Text = _settings.WorkDuration.ToString();
            BreakDurationBox.Text = _settings.BreakDuration.ToString();
            
            AlphaSlider.Value = _settings.BackgroundAlpha;
            WindowSizeSlider.Value = _settings.WindowSize;
            BoldCheck.IsChecked = _settings.IsBold;
            
            // 初始化字型選單
            FontFamilyCombo.ItemsSource = Fonts.SystemFontFamilies.OrderBy(f => f.Source);
            var currentFont = Fonts.SystemFontFamilies.FirstOrDefault(f => f.Source == _settings.FontFamily);
            if (currentFont != null) FontFamilyCombo.SelectedItem = currentFont;

            SoundPathBox.Text = _settings.SoundPath;
            TextColorBox.Text = _settings.TextColor;
            WorkColorBox.Text = _settings.WorkColor;
            BreakColorBox.Text = _settings.BreakColor;
            VolumeSlider.Value = _settings.Volume;

            // 即時預覽設定 (Live Preview)
            AlphaSlider.ValueChanged += (s, e) => _settings.BackgroundAlpha = (byte)AlphaSlider.Value;
            WindowSizeSlider.ValueChanged += (s, e) => _settings.WindowSize = WindowSizeSlider.Value;
            BoldCheck.Checked += (s, e) => _settings.IsBold = true;
            BoldCheck.Unchecked += (s, e) => _settings.IsBold = false;
            
            FontFamilyCombo.SelectionChanged += (s, e) => 
            {
                if (FontFamilyCombo.SelectedItem is FontFamily selectedFont)
                    _settings.FontFamily = selectedFont.Source;
            };

            // 初始化選色器
            InitializeColorPicker(TextColorPresets, TextColorBox, TextColorPreview, c => _settings.TextColor = c, _settings.TextColor, true);
            InitializeColorPicker(WorkColorPresets, WorkColorBox, WorkColorPreview, c => _settings.WorkColor = c, _settings.WorkColor);
            InitializeColorPicker(BreakColorPresets, BreakColorBox, BreakColorPreview, c => _settings.BreakColor = c, _settings.BreakColor);

            VolumeSlider.ValueChanged += (s, e) => _settings.Volume = VolumeSlider.Value;
        }

        private void InitializeColorPicker(WrapPanel panel, TextBox box, Border preview, Action<string> updateAction, string initialValue, bool isText = false)
        {
            string[] presets = isText 
                ? new[] { "#FFFFFF", "#F0F0F0", "#CCCCCC", "#999999", "#333333", "#000000", "#FFD700", "#FF69B4" }
                : new[] { "#FF8C00", "#FF4500", "#32CD32", "#008000", "#1E90FF", "#0000FF", "#8A2BE2", "#A9A9A9" };

            var brushConverter = new BrushConverter();

            void UpdateUI(string hex)
            {
                try
                {
                    var brush = brushConverter.ConvertFromString(hex) as SolidColorBrush;
                    preview.Background = brush;
                    if (box.Text != hex) box.Text = hex;
                    updateAction(hex);
                }
                catch { /* Ignore invalid hex */ }
            }

            foreach (var color in presets)
            {
                var btn = new Button
                {
                    Background = brushConverter.ConvertFromString(color) as Brush,
                    Style = (Style)FindResource("ColorCircleStyle"),
                    Tag = color
                };
                btn.Click += (s, e) => UpdateUI((string)((Button)s).Tag);
                panel.Children.Add(btn);
            }

            box.TextChanged += (s, e) => UpdateUI(box.Text);
            UpdateUI(initialValue);
        }

        private void BrowseSound_Click(object sender, RoutedEventArgs e)
        {
            var dialog = new OpenFileDialog
            {
                Filter = "WAV Files (*.wav)|*.wav|All Files (*.*)|*.*",
                InitialDirectory = AppDomain.CurrentDomain.BaseDirectory
            };

            if (dialog.ShowDialog() == true)
            {
                _settings.SoundPath = dialog.FileName;
                SoundPathBox.Text = _settings.SoundPath;
            }
        }

        private void Save_Click(object sender, RoutedEventArgs e)
        {
            if (int.TryParse(WorkDurationBox.Text, out int work) && int.TryParse(BreakDurationBox.Text, out int brk))
            {
                _settings.WorkDuration = work;
                _settings.BreakDuration = brk;
                _settings.Save();
                this.Close();
            }
            else
            {
                MessageBox.Show("Please enter valid numbers for duration.");
            }
        }
    }
}