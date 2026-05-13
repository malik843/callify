package com.callify.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Foreground service that listens for phone state changes.
 * This service remains dormant until a call is detected, at which point it
 * extracts the phone number and coordinates the lookup and overlay display.
 */
class CallDetectorService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
