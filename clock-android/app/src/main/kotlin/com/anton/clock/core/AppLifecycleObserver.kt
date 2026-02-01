package com.anton.clock.core

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppLifecycleObserver : LifecycleEventObserver {
    
    private val _isForeground = MutableStateFlow(false)
    val isForeground = _isForeground.asStateFlow()

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                _isForeground.value = true
            }
            Lifecycle.Event.ON_STOP -> {
                _isForeground.value = false
            }
            else -> {}
        }
    }

    companion object {
        private var instance: AppLifecycleObserver? = null
        
        fun getInstance(): AppLifecycleObserver {
            if (instance == null) {
                instance = AppLifecycleObserver()
            }
            return instance!!
        }
    }
}
