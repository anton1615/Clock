using System;
using System.Windows;
using clock.Services;
using clock.ViewModels;
using clock.Views;

namespace clock
{
    public partial class App : Application
    {
        public App()
        {
            // 捕捉未處理的例外
            this.DispatcherUnhandledException += App_DispatcherUnhandledException;
            AppDomain.CurrentDomain.UnhandledException += CurrentDomain_UnhandledException;
        }

        protected override void OnStartup(StartupEventArgs e)
        {
            base.OnStartup(e);

            var settingsService = new SettingsService();
            var timer = new WpfTimer();
            var mainViewModel = new MainViewModel(settingsService, timer);
            var mainWindow = new MainWindow();
            mainWindow.DataContext = mainViewModel;
            mainWindow.Show();
        }

        private void App_DispatcherUnhandledException(object sender, System.Windows.Threading.DispatcherUnhandledExceptionEventArgs e)
        {
            Console.WriteLine($"[UI Error] {e.Exception.Message}");
            MessageBox.Show($"UI Error: {e.Exception.Message}", "Crash");
            e.Handled = true; // 嘗試讓程式繼續運行
        }

        private void CurrentDomain_UnhandledException(object sender, UnhandledExceptionEventArgs e)
        {
            var ex = e.ExceptionObject as Exception;
            Console.WriteLine($"[System Error] {ex?.Message}");
            MessageBox.Show($"System Error: {ex?.Message}", "Critical Crash");
        }
    }
}