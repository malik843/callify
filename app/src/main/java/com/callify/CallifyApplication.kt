package com.callify

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.callify.data.local.CallifyDatabase
import com.callify.data.local.DatabaseSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Main application class for Callify.
 * Initialises Hilt for dependency injection and creates all notification channels.
 */
@HiltAndroidApp
class CallifyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Seed the local mock database on first launch.
        CoroutineScope(Dispatchers.IO).launch {
            val db = CallifyDatabase.getInstance(this@CallifyApplication)
            DatabaseSeeder.seedIfEmpty(db.callerDao())
        }

        createNotificationChannels()
    }

    /**
     * Creates all notification channels used by Callify.
     *
     * - [CHANNEL_INCOMING_CALL] — IMPORTANCE_HIGH, public lock-screen visibility,
     *   used by [com.callify.service.CallifyInCallService] for incoming call alerts.
     *
     * The service-status channel for [com.callify.service.CallDetectorService]
     * is created inside that service itself (IMPORTANCE_LOW persistent notification).
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val incomingCallChannel = NotificationChannel(
                CHANNEL_INCOMING_CALL,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Callify incoming call alerts"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(incomingCallChannel)
        }
    }

    companion object {
        const val CHANNEL_INCOMING_CALL = "callify_incoming_call"
    }
}
