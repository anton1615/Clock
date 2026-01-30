using System;
using System.Globalization;
using System.Windows.Data;
using System.Windows.Media;
using clock.Core;
using clock.Models;

namespace clock.Converters
{
    /// <summary>
    /// 根據 PomodoroEngine 的階段與 AppSettings 的設定回傳對應的顏色。
    /// </summary>
    public class PhaseColorConverter : IMultiValueConverter
    {
        public object Convert(object[] values, Type targetType, object parameter, CultureInfo culture)
        {
            if (values.Length < 2 || values[0] is not PomodoroEngine engine || values[1] is not AppSettings settings)
                return Brushes.Orange;

            string colorHex = engine.IsWorkPhase ? settings.WorkColor : settings.BreakColor;
            
            try
            {
                var color = (Color)ColorConverter.ConvertFromString(colorHex);
                
                // 如果參數是 "Background"，則返回深色背景
                if (parameter?.ToString() == "Background")
                {
                    var darkColor = Color.FromArgb(
                        settings.BackgroundAlpha,
                        (byte)(color.R * 0.3),
                        (byte)(color.G * 0.3),
                        (byte)(color.B * 0.3)
                    );
                    return new SolidColorBrush(darkColor);
                }

                return new SolidColorBrush(color);
            }
            catch
            {
                return Brushes.Orange;
            }
        }

        public object[] ConvertBack(object value, Type[] targetTypes, object parameter, CultureInfo culture)
            => throw new NotImplementedException();
    }

    /// <summary>
    /// 將 Hex 字串轉換為 SolidColorBrush。
    /// </summary>
    public class HexToBrushConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            string hex = value?.ToString() ?? "White";
            try
            {
                return new BrushConverter().ConvertFromString(hex) as SolidColorBrush ?? Brushes.White;
            }
            catch { return Brushes.White; }
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
            => throw new NotImplementedException();
    }

    /// <summary>
    /// 將布林值轉換為 FontWeight。
    /// </summary>
    public class BoolToFontWeightConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            bool isBold = value is bool b && b;
            return isBold ? System.Windows.FontWeights.Bold : System.Windows.FontWeights.Normal;
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
            => throw new NotImplementedException();
    }
}