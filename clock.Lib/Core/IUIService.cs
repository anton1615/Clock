namespace clock.Core
{
    /// <summary>
    /// 定義 UI 相關的控制行為（如退出、隱藏/顯示視窗）。
    /// </summary>
    public interface IUIService
    {
        /// <summary>
        /// 關閉整個應用程式。
        /// </summary>
        void ExitApp();

        /// <summary>
        /// 切換主視窗的顯示狀態（顯示/隱藏）。
        /// </summary>
        void ToggleMainWindow();
    }
}