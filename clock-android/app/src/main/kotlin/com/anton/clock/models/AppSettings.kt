package com.anton.clock.models

data class AppSettings(
    val workDuration: Int = 25,
    val breakDuration: Int = 5,
    val workColor: String = "#FF8C00",
    val breakColor: String = "#32CD32",
    val soundUri: String? = "default", // "default", "silent", or specific URI
    val keepScreenOn: Boolean = false
)
