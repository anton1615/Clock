using System;
using System.Globalization;
using System.Windows;
using System.Windows.Data;

namespace clock.ViewModels
{
    public class EmSizeConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            if (value is double baseSize)
            {
                string param = parameter?.ToString() ?? "1.0";

                // 處理 Thickness (Margin/Padding) 格式: "left,top,right,bottom"
                if (param.Contains(","))
                {
                    var parts = param.Split(',');
                    if (parts.Length == 4)
                    {
                        return new Thickness(
                            baseSize * double.Parse(parts[0]),
                            baseSize * double.Parse(parts[1]),
                            baseSize * double.Parse(parts[2]),
                            baseSize * double.Parse(parts[3])
                        );
                    }
                    // 處理單一數值轉 Thickness
                    double uniform = baseSize * double.Parse(parts[0]);
                    return new Thickness(uniform);
                }

                // 處理一般數值 (Width/Height/FontSize)
                if (double.TryParse(param, out double ratio))
                {
                    // 如果目標是 Thickness 類型但參數只是單一數字
                    if (targetType == typeof(Thickness))
                        return new Thickness(baseSize * ratio);
                        
                    return Math.Max(1, baseSize * ratio);
                }
            }
            return value;
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            throw new NotImplementedException();
        }
    }
}