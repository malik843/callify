package com.callify

import android.app.Application
import com.callify.data.local.CallifyDatabase
import com.callify.data.local.DatabaseSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Main application class for Callify.
 * Initialises Hilt for dependency injection.
 */
@HiltAndroidApp
class CallifyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Seed the database on first launch
        CoroutineScope(Dispatchers.IO).launch {
            val db = CallifyDatabase.getInstance(this@CallifyApplication)
            DatabaseSeeder.seedIfEmpty(db.callerDao())
        }
    }
}
