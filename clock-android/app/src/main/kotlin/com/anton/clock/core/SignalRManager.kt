package com.anton.clock.core

import android.util.Log
import com.anton.clock.models.EngineState
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class SignalRManager {
    private var hubConnection: HubConnection? = null
    
    private val _connectionState = MutableStateFlow(HubConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _lastState = MutableStateFlow<EngineState?>(null)
    val lastState = _lastState.asStateFlow()

    suspend fun connect(ip: String, port: Int = 8888): String? {
        disconnect()

        val url = "http://$ip:$port/clockhub"
        val newConnection = HubConnectionBuilder.create(url).build()

        newConnection.on("ReceiveState", { state: EngineState ->
            _lastState.value = state
        }, EngineState::class.java)

        // 核心修正：監聽伺服器關閉事件
        newConnection.onClosed { 
            Log.d("ClockSync", "SignalR connection was closed by server")
            _connectionState.value = HubConnectionState.DISCONNECTED 
            _lastState.value = null
        }

        return suspendCancellableCoroutine { continuation ->
            _connectionState.value = HubConnectionState.CONNECTING
            try {
                newConnection.start().subscribe({
                    hubConnection = newConnection
                    _connectionState.value = HubConnectionState.CONNECTED
                    hubConnection?.send("RequestState")
                    if (continuation.isActive) continuation.resume(null)
                }, { throwable ->
                    _connectionState.value = HubConnectionState.DISCONNECTED
                    if (continuation.isActive) continuation.resume(throwable.message)
                })
            } catch (e: Exception) {
                _connectionState.value = HubConnectionState.DISCONNECTED
                if (continuation.isActive) continuation.resume(e.message)
            }
        }
    }

    fun togglePause() { hubConnection?.let { if (it.connectionState == HubConnectionState.CONNECTED) it.send("TogglePause") } }
    fun togglePhase() { hubConnection?.let { if (it.connectionState == HubConnectionState.CONNECTED) it.send("TogglePhase") } }
    
    fun disconnect() {
        try { hubConnection?.stop() } catch (e: Exception) {}
        hubConnection = null
        _connectionState.value = HubConnectionState.DISCONNECTED
    }
}