namespace clock.Core
{
    /// <summary>
    /// 不執行任何操作的音效服務，用於預設值或不支援音效的平台。
    /// </summary>
    public class NullAudioService : IAudioService
    {
        public void Play(string path, double volume) { /* Do nothing */ }
    }
}