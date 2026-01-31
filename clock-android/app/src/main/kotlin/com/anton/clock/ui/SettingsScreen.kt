package com.anton.clock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anton.clock.models.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentSettings: AppSettings,
    isSynced: Boolean,
    onSave: (AppSettings) -> Unit,
    onBack: () -> Unit
) {
    var workMins by remember { mutableStateOf(currentSettings.workDuration.toFloat()) }
    var breakMins by remember { mutableStateOf(currentSettings.breakDuration.toFloat()) }
    var workColor by remember { mutableStateOf(currentSettings.workColor) }
    var breakColor by remember { mutableStateOf(currentSettings.breakColor) }
    var keepScreenOn by remember { mutableStateOf(currentSettings.keepScreenOn) }
    var soundUri by remember(currentSettings.soundUri) { mutableStateOf(currentSettings.soundUri) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val soundName = remember(soundUri) {
        when (soundUri) {
            "silent" -> "Silent"
            "default", null -> "System Default"
            else -> {
                try {
                    val ringtone = android.media.RingtoneManager.getRingtone(context, android.net.Uri.parse(soundUri))
                    ringtone?.getTitle(context) ?: "Unknown Sound"
                } catch (e: Exception) { "Custom Sound" }
            }
        }
    }

    val colors = listOf(
        "#FF8C00", "#FF4500", "#FFD700", // Oranges/Yellow
        "#32CD32", "#008000", "#ADFF2F", // Greens
        "#1E90FF", "#0000FF", "#87CEEB", // Blues
        "#8A2BE2", "#FF69B4", "#FFFFFF"  // Purple/Pink/White
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A1A1A))
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Durations", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                if (isSynced) {
                    Text("Synced with PC", color = Color(0xFFFF8C00), fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Work: ${workMins.toInt()} min", color = if (isSynced) Color.DarkGray else Color.Gray)
            Slider(
                value = workMins,
                onValueChange = { workMins = it },
                valueRange = 1f..60f,
                steps = 59,
                enabled = !isSynced,
                colors = SliderDefaults.colors(
                    thumbColor = Color(android.graphics.Color.parseColor(workColor)),
                    disabledThumbColor = Color.DarkGray
                )
            )

            Text("Break: ${breakMins.toInt()} min", color = if (isSynced) Color.DarkGray else Color.Gray)
            Slider(
                value = breakMins,
                onValueChange = { breakMins = it },
                valueRange = 1f..30f,
                steps = 29,
                enabled = !isSynced,
                colors = SliderDefaults.colors(
                    thumbColor = Color(android.graphics.Color.parseColor(breakColor)),
                    disabledThumbColor = Color.DarkGray
                )
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), color = Color.DarkGray)

            Text("Accent Colors", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Work Phase Color", color = Color.Gray)
            ColorPickerRow(selectedColor = workColor, colors = colors, onColorSelected = { workColor = it })

            Spacer(modifier = Modifier.height(16.dp))

            Text("Break Phase Color", color = Color.Gray)
            ColorPickerRow(selectedColor = breakColor, colors = colors, onColorSelected = { breakColor = it })

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), color = Color.DarkGray)

            Text("Sound & Display", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        // Triggered via MainActivity callback
                        onSave(AppSettings(
                            workDuration = workMins.toInt(),
                            breakDuration = breakMins.toInt(),
                            workColor = workColor,
                            breakColor = breakColor,
                            keepScreenOn = keepScreenOn,
                            soundUri = "PICK_SOUND" // Special trigger
                        ))
                    }
                    .padding(vertical = 12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Notification Sound", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        text = soundName, 
                        color = Color.Gray, 
                        fontSize = 12.sp
                    )
                }
                Text("›", color = Color.Gray, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Keep Screen On", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Prevent screen from dimming while app is in focus", color = Color.Gray, fontSize = 12.sp)
                }
                Switch(
                    checked = keepScreenOn,
                    onCheckedChange = { keepScreenOn = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF8C00))
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    onSave(AppSettings(
                        workDuration = workMins.toInt(),
                        breakDuration = breakMins.toInt(),
                        workColor = workColor,
                        breakColor = breakColor,
                        keepScreenOn = keepScreenOn,
                        soundUri = soundUri
                    ))
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C00))
            ) {
                Text("SAVE SETTINGS", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun HorizontalDivider(modifier: Modifier = Modifier, color: Color = Color.Gray) {
    Box(modifier = modifier.fillMaxWidth().height(1.dp).background(color))
}

@Composable
fun ColorPickerRow(selectedColor: String, colors: List<String>, onColorSelected: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(colors) { hex ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(hex)))
                    .clickable { onColorSelected(hex) }
                    .then(
                        if (selectedColor == hex) Modifier.background(Color.White.copy(alpha = 0.3f), CircleShape)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selectedColor == hex) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color.White))
                }
            }
        }
    }
}
