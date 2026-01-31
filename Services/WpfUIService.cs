using System.Windows;
using clock.Core;
using System.Threading.Tasks;

namespace clock.Services
{
    public class WpfUIService : IUIService
    {
        private readonly Func<Task> _stopServerAction;

        public WpfUIService(Func<Task> stopServerAction)
        {
            _stopServerAction = stopServerAction;
        }

        public async void ExitApp()
        {
            // 先異步停止伺服器，通知所有客戶端斷開
            await _stopServerAction();
            Application.Current.Shutdown();
        }

        public void ToggleMainWindow()
        {
            var window = Application.Current.MainWindow;
            if (window != null)
            {
                if (window.Visibility == Visibility.Visible) window.Hide();
                else { window.Show(); window.Activate(); }
            }
        }
    }
}