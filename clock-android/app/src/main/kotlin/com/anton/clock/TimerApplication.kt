package com.anton.clock

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.anton.clock.core.AppLifecycleObserver

class TimerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver.getInstance())
    }
}
