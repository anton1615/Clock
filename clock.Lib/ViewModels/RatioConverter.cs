using System;
using System.Globalization;
using System.Windows.Data;

namespace clock.ViewModels
{
    // 用於維持視窗長寬比
    // Input: Width (double)
    // Output: Height (double) = Width * 0.45
    public class RatioConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            if (value is double width)
            {
                return width * 0.45; 
            }
            return 100.0;
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            throw new NotImplementedException();
        }
    }
}