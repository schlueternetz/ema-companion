package com.schlueternetz.emacompanion

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.schlueternetz.emacompanion.core.api.AppForegroundTracker

class EmaCompanionApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppForegroundTracker.create(this))
    }
}
