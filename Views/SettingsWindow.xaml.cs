using System;
using System.Windows;
using System.Windows.Controls;
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
            
            FontFamilyBox.Text = _settings.FontFamily;
            TextColorBox.Text = _settings.TextColor;
            WorkColorBox.Text = _settings.WorkColor;
            BreakColorBox.Text = _settings.BreakColor;
            VolumeSlider.Value = _settings.Volume;

            // 即時預覽設定 (Live Preview)
            AlphaSlider.ValueChanged += (s, e) => _settings.BackgroundAlpha = (byte)AlphaSlider.Value;
            WindowSizeSlider.ValueChanged += (s, e) => _settings.WindowSize = WindowSizeSlider.Value;
            BoldCheck.Checked += (s, e) => _settings.IsBold = true;
            BoldCheck.Unchecked += (s, e) => _settings.IsBold = false;
            
            FontFamilyBox.TextChanged += (s, e) => _settings.FontFamily = FontFamilyBox.Text;
            TextColorBox.TextChanged += (s, e) => _settings.TextColor = TextColorBox.Text;
            WorkColorBox.TextChanged += (s, e) => _settings.WorkColor = WorkColorBox.Text;
            BreakColorBox.TextChanged += (s, e) => _settings.BreakColor = BreakColorBox.Text;
            VolumeSlider.ValueChanged += (s, e) => _settings.Volume = VolumeSlider.Value;
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