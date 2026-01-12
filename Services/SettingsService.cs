using clock.Core;
using clock.Models;
using clock.Views;

namespace clock.Services
{
    public class SettingsService : ISettingsService
    {
        public void OpenSettings(AppSettings settings)
        {
            var settingsWindow = new SettingsWindow(settings);
            settingsWindow.ShowDialog();
        }
    }
}
