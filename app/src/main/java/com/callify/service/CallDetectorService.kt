package com.callify.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.callify.BuildConfig
import com.callify.data.model.CallerResult
import com.callify.overlay.OverlayManager
import com.callify.repository.CallerRepository
import com.callify.utils.PermissionHelper
import com.callify.utils.PhoneNumberNormalizer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Foreground service that listens for phone state changes.
 *
 * This service remains dormant until a call is detected. It uses [TelephonyManager]
 * to listen for ringing, off-hook, and idle states to coordinate caller identification.
 */
@AndroidEntryPoint
class CallDetectorService : Service() {

    @Inject
    lateinit var callerRepository: CallerRepository

    @Inject
    lateinit var overlayManager: OverlayManager

    private lateinit var telephonyManager: TelephonyManager
    private var phoneStateListener: CallifyPhoneStateListener? = null
    private var telephonyCallback: TelephonyCallback? = null

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lookupJob: Job? = null
    
    /**
     * Flag to prevent slow API responses from rendering the overlay 
     * after the call has already ended.
     */
    private var isCallActive = false

    companion object {
        private const val TAG = "Callify"
        private const val CHANNEL_ID = "callify_service_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Log.d(TAG, "CallDetectorService created")
        
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }

        if (!PermissionHelper.hasPhoneStatePermission(this)) {
            Log.e(TAG, "READ_PHONE_STATE permission missing. Stopping service.")
            stopSelf()
            return
        }

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        registerCallListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (BuildConfig.DEBUG) Log.d(TAG, "CallDetectorService started with ID: $startId")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        if (BuildConfig.DEBUG) Log.d(TAG, "CallDetectorService destroyed")
        unregisterCallListener()
        overlayManager.dismiss()
        serviceScope.cancel()
    }

    /**
     * Registers the appropriate listener for call states based on Android version.
     */
    private fun registerCallListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handleCallState(state, null)
                }
            }
            telephonyCallback = callback
            telephonyManager.registerTelephonyCallback(mainExecutor, callback)
        } else {
            val listener = CallifyPhoneStateListener()
            phoneStateListener = listener
            @Suppress("DEPRECATION")
            telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    /**
     * Unregisters the call state listener.
     */
    private fun unregisterCallListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { telephonyManager.unregisterTelephonyCallback(it) }
        } else {
            phoneStateListener?.let {
                @Suppress("DEPRECATION")
                telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
            }
        }
    }

    /**
     * Handles transitions between call states.
     *
     * @param state The current call state (RINGING, OFFHOOK, or IDLE).
     * @param incomingNumber The phone number for RINGING state (might be null on some API levels).
     */
    private fun handleCallState(state: Int, incomingNumber: String?) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                // Edge Case: Rapid sequential calls (call waiting)
                lookupJob?.cancel()
                overlayManager.dismiss()

                val normalized = PhoneNumberNormalizer.normalize(incomingNumber)
                
                if (BuildConfig.DEBUG) Log.d(TAG, "Incoming: $incomingNumber (normalized: $normalized)")

                // Edge Case: Private / Withheld numbers
                if (normalized == null || 
                    normalized == "-1" || 
                    normalized.equals("unknown", ignoreCase = true) ||
                    normalized.equals("private", ignoreCase = true)) {
                    
                    if (BuildConfig.DEBUG) Log.d(TAG, "Private or unknown number detected. No API lookup.")
                    if (PermissionHelper.hasOverlayPermission(applicationContext)) {
                        overlayManager.show(CallerResult.NotFound)
                    }
                    return
                }

                if (!PermissionHelper.hasOverlayPermission(applicationContext)) {
                    Log.w(TAG, "Overlay permission revoked — cannot show caller ID")
                    return
                }

                isCallActive = true
                overlayManager.showLoading()
                
                lookupJob = serviceScope.launch {
                    // ── API TRIGGER ──────────────────────────────────────────────────
                    // This is the only location in the service layer that initiates
                    // a Callify API call. Fires on CALL_STATE_RINGING after:
                    //   1. isCallActive guard set to true
                    //   2. Number validated and normalised by PhoneNumberNormalizer
                    //   3. showLoading() overlay already rendered to the user
                    // ─────────────────────────────────────────────────────────────────
                    val result = callerRepository.lookup(normalized)
                    
                    // Thread Safety: Ensure UI updates happen on the Main thread
                    withContext(Dispatchers.Main) {
                        // Race condition guard: Check if call is still active
                        if (!isCallActive) {
                            if (BuildConfig.DEBUG) Log.d(TAG, "Lookup returned but call is no longer active. Skipping overlay.")
                            return@withContext
                        }
                        
                        overlayManager.show(result)
                        
                        if (BuildConfig.DEBUG) {
                            when (result) {
                                is CallerResult.Found -> Log.d(TAG, "Found: ${result.info.firstname} ${result.info.lastname}")
                                is CallerResult.NotFound -> Log.d(TAG, "No record for $normalized")
                                is CallerResult.Timeout -> Log.d(TAG, "Lookup timed out")
                                is CallerResult.NetworkError -> Log.d(TAG, "Network error during lookup")
                            }
                        }
                    }
                }
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (BuildConfig.DEBUG) Log.d(TAG, "Call answered or outgoing")
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (BuildConfig.DEBUG) Log.d(TAG, "Call ended — teardown")
                isCallActive = false
                lookupJob?.cancel()
                overlayManager.dismiss()
            }
        }
    }

    /**
     * Creates the notification channel required for the foreground service.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Callify Active",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for Callify active call detection"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Creates the persistent notification for the foreground service.
     *
     * @return The configured [Notification].
     */
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Callify")
            .setContentText("Callify is listening for incoming calls")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    /**
     * Internal listener implementation for API versions below S.
     */
    private inner class CallifyPhoneStateListener : PhoneStateListener() {
        @Deprecated("Deprecated in Java")
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            handleCallState(state, phoneNumber)
        }
    }
}
