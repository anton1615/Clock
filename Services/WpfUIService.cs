using System.Windows;
using clock.Core;

namespace clock.Services
{
    public class WpfUIService : IUIService
    {
        public void ExitApp()
        {
            Application.Current.Shutdown();
        }

        public void ToggleMainWindow()
        {
            var window = Application.Current.MainWindow;
            if (window != null)
            {
                if (window.Visibility == Visibility.Visible)
                {
                    window.Hide();
                }
                else
                {
                    window.Show();
                    window.Activate();
                }
            }
        }
    }
}