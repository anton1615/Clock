using Microsoft.AspNetCore.SignalR;
using clock.Core;
using clock.Models;
using System.Threading.Tasks;

namespace clock.Hubs
{
    public class ClockHub : Hub
    {
        private readonly PomodoroEngine _engine;

        public ClockHub(PomodoroEngine engine)
        {
            _engine = engine;
        }

        // 當手機剛連上時，會呼叫這個方法
        public async Task RequestState()
        {
            // 立即回傳目前的狀態給發起請求的客戶端
            await Clients.Caller.SendAsync("ReceiveState", _engine.GetState());
        }

        public void TogglePause() => _engine.TogglePause();
        public void TogglePhase() => _engine.TogglePhase();
    }
}