package com.example.callify

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main application class for Callify.
 * Initialises Hilt for dependency injection.
 */
@HiltAndroidApp
class CallifyApplication : Application()
