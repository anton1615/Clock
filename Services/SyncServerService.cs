using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using clock.Core;
using clock.Hubs;
using Microsoft.AspNetCore.SignalR;
using System.Threading.Tasks;
using System;

namespace clock.Services
{
    public class SyncServerService
    {
        private WebApplication? _app;
        private readonly PomodoroEngine _engine;

        public SyncServerService(PomodoroEngine engine)
        {
            _engine = engine;
            
            // 訂閱引擎變更，即時推送給所有客戶端
            _engine.PropertyChanged += async (s, e) =>
            {
                if (_app != null)
                {
                    var hubContext = _app.Services.GetRequiredService<IHubContext<ClockHub>>();
                    await hubContext.Clients.All.SendAsync("ReceiveState", _engine.GetState());
                }
            };
        }

        public void Start()
        {
            Task.Run(async () =>
            {
                try
                {
                    var builder = WebApplication.CreateBuilder();
                    
                    // 設定監聽端口 (預設 5000)
                    builder.WebHost.ConfigureKestrel(options =>
                    {
                        options.ListenAnyIP(5000);
                    });

                    builder.Services.AddSignalR();
                    builder.Services.AddSingleton(_engine);
                    builder.Services.AddCors(options =>
                    {
                        options.AddDefaultPolicy(policy =>
                        {
                            policy.AllowAnyHeader()
                                  .AllowAnyMethod()
                                  .SetIsOriginAllowed(_ => true)
                                  .AllowCredentials();
                        });
                    });

                    _app = builder.Build();

                    _app.UseCors();
                    _app.MapHub<ClockHub>("/clockhub");

                    await _app.RunAsync();
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"[SyncServer] Start failed: {ex.Message}");
                }
            });
        }

        public async Task StopAsync()
        {
            if (_app != null)
            {
                await _app.StopAsync();
            }
        }
    }
}