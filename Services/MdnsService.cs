using Makaretu.Dns;
using System;
using System.Net;
using System.Net.Sockets;
using System.Linq;
using System.Collections.Generic;
using System.Net.NetworkInformation;

namespace clock.Services
{
    public class MdnsService : IDisposable
    {
        private MulticastService? _mdns;
        private ServiceDiscovery? _sd;
        private readonly string _instanceName;

        public MdnsService(string instanceName = "ClockPC")
        {
            _instanceName = instanceName;
        }

        public void Start()
        {
            Task.Run(() =>
            {
                try
                {
                    _mdns = new MulticastService();
                    _sd = new ServiceDiscovery(_mdns);
                    
                    // 1. 取得所有實體網卡的 IPv4
                    var ips = new List<IPAddress>();
                    foreach (var ni in NetworkInterface.GetAllNetworkInterfaces())
                    {
                        // 過濾：必須是正在運作的、非回環的、且排除虛擬網卡
                        if (ni.OperationalStatus == OperationalStatus.Up && 
                            ni.NetworkInterfaceType != NetworkInterfaceType.Loopback)
                        {
                            var desc = ni.Description.ToLower();
                            if (desc.Contains("vmware") || desc.Contains("virtualbox") || desc.Contains("pseudo"))
                                continue;

                            var props = ni.GetIPProperties();
                            foreach (var addr in props.UnicastAddresses)
                            {
                                if (addr.Address.AddressFamily == AddressFamily.InterNetwork)
                                {
                                    ips.Add(addr.Address);
                                }
                            }
                        }
                    }

                    // 2. 建立服務設定
                    var profile = new ServiceProfile(_instanceName, "_clock._tcp", 8888, ips);
                    profile.AddProperty("version", "1.1.4");

                    _sd.Advertise(profile);
                    _mdns.Start();

                    Console.WriteLine("========================================");
                    Console.WriteLine($"[mDNS] Advertising on {ips.Count} real interfaces:");
                    foreach(var ip in ips) Console.WriteLine($"  - {ip}");
                    Console.WriteLine("========================================");
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"[mDNS] Start failed: {ex.Message}");
                }
            });
        }

        public void Dispose()
        {
            _sd?.Dispose();
            _mdns?.Stop();
            _mdns?.Dispose();
        }
    }
}