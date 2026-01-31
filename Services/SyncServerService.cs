using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.DependencyInjection;
using clock.Core;
using clock.Hubs;
using Microsoft.AspNetCore.SignalR;
using System.Threading.Tasks;
using System;
using System.Linq;
using System.Net;
using Microsoft.Extensions.Logging;

namespace clock.Services
{
    public class SyncServerService
    {
        private WebApplication? _app;
        private PomodoroEngine _engine;

        public SyncServerService(PomodoroEngine engine)
        {
            _engine = engine;
            BindEvents();
            
            // 將廣播頻率降為 5 秒 (僅作為斷線容錯與對時校準)
            Task.Run(async () => {
                while (true) {
                    await Task.Delay(5000);
                    BroadcastState();
                }
            });
        }

        public void UpdateEngine(PomodoroEngine newEngine)
        {
            _engine = newEngine;
            BindEvents();
        }

        private void BindEvents()
        {
            // 當按鈕被點擊時，會立即觸發一次廣播，保證毫秒級反應
            _engine.OnSignificantStateChanged += BroadcastState;
        }

        private async void BroadcastState()
        {
            if (_app != null)
            {
                try {
                    var hubContext = _app.Services.GetRequiredService<IHubContext<ClockHub>>();
                    await hubContext.Clients.All.SendAsync("ReceiveState", _engine.GetState());
                } catch {}
            }
        }

        public void Start()
        {
            Task.Run(async () =>
            {
                try
                {
                    var builder = WebApplication.CreateBuilder();
                    builder.Logging.ClearProviders();
                    builder.WebHost.ConfigureKestrel(o => o.Listen(IPAddress.Any, 8888));
                    builder.Services.AddSignalR();
                    builder.Services.AddSingleton(_engine);
                    builder.Services.AddCors(o => o.AddDefaultPolicy(p => p.AllowAnyHeader().AllowAnyMethod().SetIsOriginAllowed(_ => true).AllowCredentials()));

                    _app = builder.Build();
                    _app.UseCors();
                    _app.MapGet("/ping", () => "Pong");
                    _app.MapHub<ClockHub>("/clockhub");

                    await _app.RunAsync();
                }
                catch (Exception ex) { Console.WriteLine($"[SyncServer] FATAL: {ex.Message}"); }
            });
        }

        public async Task StopAsync()
        {
            if (_app != null) await _app.StopAsync();
        }
    }
}