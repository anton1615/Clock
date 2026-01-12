using System;
using System.Globalization;
using System.Windows.Data;

namespace clock.ViewModels
{
    // 通用的倍率轉換器
    // 用法：Binding Converter={StaticResource Multiplier}, ConverterParameter=1.5
    public class FontSizeMultiplierConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            if (value is double fontSize && parameter != null)
            {
                if (double.TryParse(parameter.ToString(), out double factor))
                {
                    return fontSize * factor;
                }
            }
            return value; // 轉換失敗則回傳原值
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            throw new NotImplementedException();
        }
    }
}