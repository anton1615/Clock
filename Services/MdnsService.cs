using Makaretu.Dns;
using System;
using System.Net;
using System.Net.Sockets;
using System.Linq;

namespace clock.Services
{
    public class MdnsService : IDisposable
    {
        private ServiceDiscovery? _sd;
        private readonly string _instanceName;

        public MdnsService(string instanceName = "ClockPC")
        {
            _instanceName = instanceName;
        }

        public void Start()
        {
            try
            {
                _sd = new ServiceDiscovery();
                
                var service = new ServiceProfile(_instanceName, "_clock._tcp", 5000);
                
                // 增加一些自定義屬性
                service.AddProperty("version", "1.1.0");
                service.AddProperty("os", Environment.OSVersion.Platform.ToString());

                _sd.Advertise(service);
                Console.WriteLine($"[mDNS] Advertising service: {_instanceName}._clock._tcp.local on port 5000");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[mDNS] Start failed: {ex.Message}");
            }
        }

        public void Dispose()
        {
            _sd?.Dispose();
        }
    }
}