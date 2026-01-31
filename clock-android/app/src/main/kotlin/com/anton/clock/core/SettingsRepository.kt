package com.anton.clock.core

import android.content.Context
import com.anton.clock.models.AppSettings

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("clock_settings", Context.MODE_PRIVATE)

    fun getSettings(): AppSettings {
        return AppSettings(
            workDuration = prefs.getInt("work_duration", 25),
            breakDuration = prefs.getInt("break_duration", 5),
            workColor = prefs.getString("work_color", "#FF8C00") ?: "#FF8C00",
            breakColor = prefs.getString("break_color", "#32CD32") ?: "#32CD32",
            soundUri = prefs.getString("sound_uri", "default"),
            keepScreenOn = prefs.getBoolean("keep_screen_on", false)
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit().apply {
            putInt("work_duration", settings.workDuration)
            putInt("break_duration", settings.breakDuration)
            putString("work_color", settings.workColor)
            putString("break_color", settings.breakColor)
            putString("sound_uri", settings.soundUri)
            putBoolean("keep_screen_on", settings.keepScreenOn)
            apply()
        }
    }
}
