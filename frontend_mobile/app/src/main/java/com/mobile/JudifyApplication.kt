package com.mobile

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class JudifyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Use system default night mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
} 