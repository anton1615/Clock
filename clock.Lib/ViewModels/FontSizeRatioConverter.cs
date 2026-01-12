using System;
using System.Globalization;
using System.Windows.Data;

namespace clock.ViewModels
{
    public class FontSizeRatioConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            if (value is double size)
            {
                return Math.Max(12, size / 3.0); // 按鈕字體是時間字體的 1/3，最小 12
            }
            return 14;
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            throw new NotImplementedException();
        }
    }
}