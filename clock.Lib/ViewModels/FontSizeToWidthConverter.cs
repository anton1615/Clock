using System;
using System.Globalization;
using System.Windows.Data;

namespace clock.ViewModels
{
    public class FontSizeToWidthConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            if (value is double settingValue)
            {
                // 倍率 4.0
                // 30 -> 120
                // 50 -> 200
                return Math.Max(120, settingValue * 4.0);
            }
            return 200.0;
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            throw new NotImplementedException();
        }
    }
}