using System;
using System.ComponentModel;
using System.Runtime.InteropServices; // 新增
using System.Windows;
using System.Windows.Input;
using System.Windows.Interop; // 新增
using System.Windows.Threading; // 新增
using clock.ViewModels;

namespace clock.Views
{
    public partial class MainWindow : Window
    {
        // --- Win32 API 定義 ---
        [DllImport("user32.dll")]
        [return: MarshalAs(UnmanagedType.Bool)]
        private static extern bool SetWindowPos(IntPtr hWnd, IntPtr hWndInsertAfter, int X, int Y, int cx, int cy, uint uFlags);

        private static readonly IntPtr HWND_TOPMOST = new IntPtr(-1);
        private const uint SWP_NOSIZE = 0x0001;
        private const uint SWP_NOMOVE = 0x0002;
        private const uint SWP_NOACTIVATE = 0x0010;
        private const uint SWP_SHOWWINDOW = 0x0040;

        private DispatcherTimer _topmostTimer = null!;
        private bool _isInitialLoad = true;

        public MainWindow()
        {
            InitializeComponent();
        }

        private void Window_Loaded(object sender, RoutedEventArgs e)
        {
            if (DataContext is MainViewModel vm)
            {
                UpdateSize(vm.WindowWidth, vm.WindowHeight);
                _isInitialLoad = false;

                vm.PropertyChanged += (s, args) =>
                {
                    if (args.PropertyName == nameof(MainViewModel.WindowWidth) ||
                        args.PropertyName == nameof(MainViewModel.WindowHeight))
                    {
                        UpdateSize(vm.WindowWidth, vm.WindowHeight);
                    }
                };
            }
            
            // 初始強制置頂
            RefreshTopmost();

            // 啟動一個計時器，每 2 秒檢查並強制置頂，確保不被工具列蓋住
            _topmostTimer = new DispatcherTimer();
            _topmostTimer.Interval = TimeSpan.FromSeconds(2);
            _topmostTimer.Tick += (s, args) => RefreshTopmost();
            _topmostTimer.Start();
        }

        // 當視窗失去焦點 (例如點擊工具列) 時觸發，立即搶回主權
        private void Window_Deactivated(object sender, EventArgs e)
        {
            RefreshTopmost();
        }

        private void RefreshTopmost()
        {
            // 方法 A: WPF 屬性 (有時候不夠力)
            this.Topmost = true;

            // 方法 B: Win32 API 強制置頂 (核彈級解法)
            try
            {
                var hwnd = new WindowInteropHelper(this).Handle;
                if (hwnd != IntPtr.Zero)
                {
                    SetWindowPos(hwnd, HWND_TOPMOST, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE);
                }
            }
            catch 
            {
                // 忽略錯誤
            }
        }

        private void UpdateSize(double width, double height)
        {
            if (width > 0 && height > 0)
            {
                this.Width = width;
                this.Height = height;
                this.InvalidateMeasure();
                
                if (_isInitialLoad)
                {
                    MoveToBottomCenter();
                }
            }
        }

        private void MoveToBottomCenter()
        {
            double screenHeight = SystemParameters.PrimaryScreenHeight;
            double screenWidth = SystemParameters.PrimaryScreenWidth;

            this.Left = (screenWidth - this.Width) / 2;
            
            // 緊貼螢幕底部，確保覆蓋工具列
            this.Top = screenHeight - this.Height;
        }

        private void BackgroundBorder_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            if (e.ButtonState == MouseButtonState.Pressed)
            {
                this.DragMove();
            }
        }

        private void Button_PreviewMouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            e.Handled = false; 
        }
    }
}