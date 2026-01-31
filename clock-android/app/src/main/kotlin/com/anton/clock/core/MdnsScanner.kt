package com.anton.clock.core

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MdnsScanner(context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null

    private val _discoveredServices = MutableStateFlow<List<NsdServiceInfo>>(emptyList())
    val discoveredServices = _discoveredServices.asStateFlow()

    private var isScanning = false

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            Log.d("ClockSync", "mDNS Scan Started: $regType")
            isScanning = true
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            Log.d("ClockSync", "Found: ${serviceInfo.serviceName}")
            // 嘗試解析服務以獲取 IP
            try {
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(si: NsdServiceInfo, errorCode: Int) {
                        Log.e("ClockSync", "Resolve failed for ${si.serviceName}: $errorCode")
                    }

                    override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                        val host = resolvedInfo.host?.hostAddress
                        Log.d("ClockSync", "Resolved: ${resolvedInfo.serviceName} at $host")
                        
                        if (host != null) {
                            val currentList = _discoveredServices.value.toMutableList()
                            if (currentList.none { it.host?.hostAddress == host }) {
                                currentList.add(resolvedInfo)
                                _discoveredServices.value = currentList
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("ClockSync", "Resolve execution error", e)
            }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            val currentList = _discoveredServices.value.toMutableList()
            currentList.removeAll { it.serviceName == serviceInfo.serviceName }
            _discoveredServices.value = currentList
        }

        override fun onDiscoveryStopped(regType: String) { isScanning = false }
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { isScanning = false }
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) { isScanning = false }
    }

    fun startScan() {
        if (isScanning) return
        try {
            if (multicastLock == null) {
                multicastLock = wifiManager.createMulticastLock("ClockLock")
            }
            multicastLock?.setReferenceCounted(false)
            if (multicastLock?.isHeld == false) multicastLock?.acquire()

            _discoveredServices.value = emptyList()
            nsdManager.discoverServices("_clock._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e("ClockSync", "Start scan error", e)
        }
    }

    fun stopScan() {
        if (!isScanning) return
        try {
            nsdManager.stopServiceDiscovery(discoveryListener)
        } catch (e: Exception) {}
        if (multicastLock?.isHeld == true) multicastLock?.release()
    }
}