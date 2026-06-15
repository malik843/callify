package com.callify.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile
import android.util.Log
import androidx.core.app.NotificationCompat
import com.callify.BuildConfig
import com.callify.CallifyApplication
import com.callify.R
import com.callify.data.model.CallerResult
import com.callify.receiver.CallActionReceiver
import com.callify.repository.CallerRepository
import com.callify.ui.CallActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Android Telecom InCallService implementation.
 *
 * Receives [Call] objects from the OS when Callify holds the default dialer
 * role ([android.app.role.RoleManager.ROLE_DIALER]).
 *
 * Responsibilities:
 * - Receive the Call on [onCallAdded] and store it in [CallManager]
 * - Perform caller lookup via [CallerRepository] (live API — no mock data)
 * - Push [CallerInfo] + call state to [CallManager] for UI consumption
 * - Launch [CallActivity] as the full-screen call screen
 * - Fire an incoming call notification with Accept / Decline actions
 * - Clean up on [onCallRemoved]
 *
 * Does NOT store call history. Does NOT log calls. No persistent writes.
 */
@AndroidEntryPoint
class CallifyInCallService : InCallService() {

    @Inject
    lateinit var callerRepository: CallerRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Callback registered per-call to forward Telecom state changes to [CallManager].
     */
    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            when (state) {
                Call.STATE_ACTIVE -> {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Call state → Active")
                    CallManager.updateState(CallState.Active)
                }
                Call.STATE_DISCONNECTED -> {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Call state → Disconnected")
                    CallManager.updateState(CallState.Disconnected)
                    cancelIncomingCallNotification()
                }
                Call.STATE_RINGING -> {
                    // Handled in onCallAdded; no duplicate action needed here.
                }
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        call.registerCallback(callCallback)
        CallManager.currentCall = call

        val rawNumber = call.details
            ?.handle
            ?.schemeSpecificPart
            .orEmpty()

        if (BuildConfig.DEBUG) Log.d(TAG, "Call added — state=${call.state}, number='$rawNumber'")

        when (call.state) {
            Call.STATE_RINGING -> {
                // Perform the API lookup, then surface the call screen and notification.
                serviceScope.launch {
                    val result = callerRepository.lookup(rawNumber)
                    val info = if (result is CallerResult.Found) result.info else null

                    if (BuildConfig.DEBUG) Log.d(TAG, "Lookup result: $result")
                    CallManager.updateState(CallState.Ringing(rawNumber, info))

                    withContext(Dispatchers.Main) {
                        launchCallActivity()
                        showIncomingCallNotification(rawNumber, info?.name)
                    }
                }
            }
            else -> {
                // Outgoing or already-active call — surface the screen immediately.
                CallManager.updateState(CallState.Active)
                launchCallActivity()
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        cancelIncomingCallNotification()
        CallManager.clear()
        serviceScope.cancel()
        if (BuildConfig.DEBUG) Log.d(TAG, "Call removed — CallManager cleared")
    }

    // ── Private helpers ────────────────────────────────────────────────────

    /** Brings [CallActivity] to the foreground over the lock screen. */
    private fun launchCallActivity() {
        startActivity(
            Intent(this, CallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
    }

    /**
     * Fires the heads-up / full-screen incoming call notification with
     * Accept and Decline action buttons.
     *
     * No call data is written to storage here or anywhere in this function.
     *
     * @param number     The raw incoming number (display only).
     * @param callerName Resolved name from the API, or null for unknown callers.
     */
    private fun showIncomingCallNotification(number: String, callerName: String?) {
        val displayName = callerName?.takeIf { it.isNotBlank() } ?: "Unknown Caller"

        val acceptIntent = PendingIntent.getBroadcast(
            this, REQUEST_CODE_ACCEPT,
            Intent(CallActionReceiver.ACTION_ACCEPT).setClass(this, CallActionReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val declineIntent = PendingIntent.getBroadcast(
            this, REQUEST_CODE_DECLINE,
            Intent(CallActionReceiver.ACTION_DECLINE).setClass(this, CallActionReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val fullScreenIntent = PendingIntent.getActivity(
            this, REQUEST_CODE_FULL_SCREEN,
            Intent(this, CallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CallifyApplication.CHANNEL_INCOMING_CALL)
            .setSmallIcon(R.drawable.ic_call)
            .setContentTitle("Incoming Call")
            .setContentText(displayName)
            .setSubText(number)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_call_accept, "Accept", acceptIntent)
            .addAction(R.drawable.ic_call_decline, "Decline", declineIntent)
            .build()

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID_INCOMING_CALL, notification)
    }

    /** Cancels the incoming call notification (call answered, declined, or ended). */
    private fun cancelIncomingCallNotification() {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(NOTIFICATION_ID_INCOMING_CALL)
    }

    companion object {
        private const val TAG = "Callify"
        private const val NOTIFICATION_ID_INCOMING_CALL = 2001
        private const val REQUEST_CODE_ACCEPT      = 100
        private const val REQUEST_CODE_DECLINE     = 101
        private const val REQUEST_CODE_FULL_SCREEN = 102
    }
}
