package com.anton.clock

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anton.clock.core.MdnsScanner
import com.anton.clock.core.PomodoroEngine
import com.anton.clock.core.SignalRManager
import com.microsoft.signalr.HubConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL

class MainActivity : ComponentActivity() {
    private val engine = PomodoroEngine()
    private lateinit var mdnsScanner: MdnsScanner
    private val signalRManager = SignalRManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mdnsScanner = MdnsScanner(this)
        engine.start()
        
        val prefs = getSharedPreferences("clock_prefs", Context.MODE_PRIVATE)
        val savedIp = prefs.getString("last_pc_ip", "") ?: ""

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val discoveredServices by mdnsScanner.discoveredServices.collectAsState()
            val connectionState by signalRManager.connectionState.collectAsState()
            val remoteState by signalRManager.lastState.collectAsState()
            val localIp = remember { getLocalIpAddress() }
            
            var showSetup by remember { mutableStateOf(false) }

            LaunchedEffect(connectionState) {
                engine.setSyncStatus(connectionState == HubConnectionState.CONNECTED)
                if (connectionState == HubConnectionState.CONNECTED) {
                    delay(500)
                    showSetup = false
                }
            }

            LaunchedEffect(showSetup, connectionState) {
                if (showSetup && connectionState == HubConnectionState.DISCONNECTED) {
                    while (isActive) {
                        mdnsScanner.startScan()
                        delay(8000)
                    }
                }
            }

            LaunchedEffect(remoteState) {
                remoteState?.let { engine.applyState(it) }
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF121212)) {
                    Box {
                        TimerScreen(
                            engine = engine, 
                            network = signalRManager,
                            isSynced = connectionState == HubConnectionState.CONNECTED,
                            onOpenSetup = { showSetup = true }
                        )

                        AnimatedVisibility(
                            visible = showSetup,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it })
                        ) {
                            SetupScreen(
                                state = connectionState,
                                devices = discoveredServices,
                                localIp = localIp,
                                initialIp = savedIp,
                                onConnect = { ip ->
                                    scope.launch {
                                        prefs.edit().putString("last_pc_ip", ip).apply()
                                        if (pingPC(ip)) signalRManager.connect(ip)
                                        else Toast.makeText(context, "Cannot reach PC.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onClose = { showSetup = false },
                                onDisconnect = { 
                                    signalRManager.disconnect()
                                    engine.reset()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun pingPC(ip: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$ip:8888/ping")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 2000
            conn.responseCode == 200
        } catch (e: Exception) { false }
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) return addr.hostAddress ?: "Unknown"
                }
            }
        } catch (e: Exception) {}
        return "Unknown"
    }

    override fun onDestroy() {
        super.onDestroy()
        mdnsScanner.stopScan()
        signalRManager.disconnect()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    state: HubConnectionState,
    devices: List<android.net.nsd.NsdServiceInfo>,
    localIp: String,
    initialIp: String,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onClose: () -> Unit
) {
    var manualIp by remember { mutableStateOf(initialIp) }
    val isConnecting = state == HubConnectionState.CONNECTING
    val isConnected = state == HubConnectionState.CONNECTED

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A)).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Sync Settings", style = MaterialTheme.typography.headlineMedium, color = Color.White, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Text("✕", color = Color.White, fontSize = 20.sp)
            }
        }
        
        Text("Phone IP: $localIp", color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(24.dp))
        
        if (!isConnected) {
            OutlinedTextField(
                value = manualIp,
                onValueChange = { manualIp = it },
                label = { Text("PC IP Address", color = Color(0xFFFF8C00)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isConnecting,
                textStyle = TextStyle(color = Color.White),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFFFF8C00),
                    unfocusedBorderColor = Color.Gray
                )
            )
            Button(
                onClick = { if(manualIp.isNotBlank()) onConnect(manualIp) },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(50.dp),
                enabled = !isConnecting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C00))
            ) {
                Text(if (isConnecting) "CONNECTING..." else "CONNECT TO PC")
            }
        } else {
            Button(
                onClick = onDisconnect,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("DISCONNECT FROM PC")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Nearby Devices (Auto)", color = Color.White, fontWeight = FontWeight.Bold)
        
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp)) {
            items(devices) { device ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { 
                        device.host?.hostAddress?.let { onConnect(it) }
                    },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(device.serviceName, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(device.host?.hostAddress ?: "Resolving...", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TimerScreen(
    engine: PomodoroEngine, 
    network: SignalRManager, 
    isSynced: Boolean,
    onOpenSetup: () -> Unit
) {
    val remainingSeconds by engine.remainingSeconds.collectAsState()
    val isWorkPhase by engine.isWorkPhase.collectAsState()
    val isPaused by engine.isPaused.collectAsState()
    
    val minutes = (remainingSeconds / 60).toInt()
    val seconds = (remainingSeconds % 60).toInt()
    val timeStr = String.format("%02d:%02d", minutes, seconds)
    
    val themeColor = if (isWorkPhase) Color(0xFFFF8C00) else Color(0xFF32CD32)
    val totalDuration = (if (isWorkPhase) engine.workDurationMinutes else engine.breakDurationMinutes) * 60.0
    val progress = if (totalDuration > 0) (remainingSeconds / totalDuration).toFloat() else 1f

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSynced) {
                Text("SYNCED", color = Color.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
            }
            IconButton(onClick = onOpenSetup, modifier = Modifier.size(32.dp)) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Sync", tint = if (isSynced) Color.Green else Color.Gray)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = themeColor.copy(alpha = 0.1f), style = Stroke(width = 12.dp.toPx()))
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = themeColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = timeStr, fontSize = 80.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(text = if (isWorkPhase) "WORK" else "BREAK", letterSpacing = 4.sp, color = themeColor, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { if (isSynced) network.togglePause() else engine.localTogglePause() },
                    modifier = Modifier.weight(1f).height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(24.dp)) {
                            if (isPaused) {
                                val path = Path().apply {
                                    moveTo(6.dp.toPx(), 2.dp.toPx())
                                    lineTo(20.dp.toPx(), 12.dp.toPx())
                                    lineTo(6.dp.toPx(), 22.dp.toPx())
                                    close()
                                }
                                drawPath(path, color = Color.White)
                            } else {
                                drawRect(color = Color.White, topLeft = Offset(6.dp.toPx(), 4.dp.toPx()), size = Size(4.dp.toPx(), 16.dp.toPx()))
                                drawRect(color = Color.White, topLeft = Offset(14.dp.toPx(), 4.dp.toPx()), size = Size(4.dp.toPx(), 16.dp.toPx()))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isPaused) "START" else "PAUSE", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    onClick = { if (isSynced) network.togglePhase() else engine.localTogglePhase() },
                    modifier = Modifier.weight(1f).height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(24.dp)) {
                            val path = Path().apply {
                                moveTo(0f, 6.dp.toPx())
                                lineTo(10.dp.toPx(), 12.dp.toPx())
                                lineTo(0f, 18.dp.toPx())
                                close()
                                moveTo(10.dp.toPx(), 6.dp.toPx())
                                lineTo(20.dp.toPx(), 12.dp.toPx())
                                lineTo(10.dp.toPx(), 18.dp.toPx())
                                close()
                            }
                            drawPath(path, color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SKIP", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}