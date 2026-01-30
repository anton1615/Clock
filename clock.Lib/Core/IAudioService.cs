namespace clock.Core
{
    /// <summary>
    /// 定義音效播放服務的介面。
    /// </summary>
    public interface IAudioService
    {
        /// <summary>
        /// 播放指定的音效檔案。
        /// </summary>
        /// <param name="path">檔案路徑。</param>
        /// <param name="volume">音量 (0.0 ~ 1.0)。</param>
        void Play(string path, double volume);
    }
}