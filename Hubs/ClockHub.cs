using Microsoft.AspNetCore.SignalR;
using clock.Core;
using clock.Models;
using System.Threading.Tasks;

namespace clock.Hubs
{
    /// <summary>
    /// SignalR Hub，用於與 Android 客戶端同步通訊。
    /// </summary>
    public class ClockHub : Hub
    {
        private readonly PomodoroEngine _engine;

        public ClockHub(PomodoroEngine engine)
        {
            _engine = engine;
        }

        /// <summary>
        /// 客戶端連線後請求當前狀態。
        /// </summary>
        public async Task RequestState()
        {
            await Clients.Caller.SendAsync("ReceiveState", _engine.GetState());
        }

        /// <summary>
        /// 遠端控制：切換暫停。
        /// </summary>
        public void TogglePause()
        {
            _engine.TogglePause();
        }

        /// <summary>
        /// 遠端控制：切換階段。
        /// </summary>
        public void TogglePhase()
        {
            _engine.TogglePhase();
        }
    }
}