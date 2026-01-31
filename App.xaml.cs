using System.Runtime.InteropServices;
using System.Windows;
using clock.Services;
using clock.ViewModels;
using clock.Views;
using clock.Core;
using clock.Models;

namespace clock
{
    public partial class App : Application
    {
        [DllImport("kernel32.dll", SetLastError = true)]
        [return: MarshalAs(UnmanagedType.Bool)]
        static extern bool AllocConsole();

        private SyncServerService? _syncServer;
        private MdnsService? _mdns;

        public App()
        {
            this.DispatcherUnhandledException += App_DispatcherUnhandledException;
            AppDomain.CurrentDomain.UnhandledException += CurrentDomain_UnhandledException;
        }

        protected override void OnStartup(StartupEventArgs e)
        {
            base.OnStartup(e);

            var settings = AppSettings.Load();
            if (settings.ShowConsole)
            {
                AllocConsole();
                Console.WriteLine("[System] Console allocated by user preference.");
            }

            var settingsService = new SettingsService();
            var timer = new WpfTimer();
            var audioService = new WpfAudioService();
            
            // 建立初始引擎
            var initialEngine = new PomodoroEngine(settings, timer);
            _syncServer = new SyncServerService(initialEngine);
            
            var uiService = new WpfUIService(async () => {
                _mdns?.Dispose();
                if (_syncServer != null) await _syncServer.StopAsync();
            });

            var mainViewModel = new MainViewModel(settingsService, timer, audioService, uiService);
            
            // 綁定正式 Engine 到伺服器
            _syncServer.UpdateEngine(mainViewModel.Engine);
            _syncServer.Start();

            _mdns = new MdnsService(Environment.MachineName);
            _mdns.Start();

            var mainWindow = new MainWindow();
            mainWindow.DataContext = mainViewModel;
            mainWindow.Show();
        }

        private void App_DispatcherUnhandledException(object sender, System.Windows.Threading.DispatcherUnhandledExceptionEventArgs e)
        {
            e.Handled = true;
        }

        private void CurrentDomain_UnhandledException(object sender, UnhandledExceptionEventArgs e)
        {
        }
    }
}