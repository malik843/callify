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
 * Android Telecom InCallService.
 *
 * Active when Callify holds [android.app.role.RoleManager.ROLE_DIALER].
 * Receives [Call] objects from the OS for every incoming and outgoing call.
 *
 * Responsibilities:
 * - Store the Call in [CallManager] and register a state callback
 * - Perform caller lookup via [CallerRepository] (local Room DB on this branch)
 * - Push caller info + state to [CallManager] for [com.callify.ui.CallActivity]
 * - Launch [com.callify.ui.CallActivity] as the full-screen call screen
 * - Fire an incoming call notification with Accept / Decline actions
 * - Clean up on [onCallRemoved]
 *
 * No call history is written anywhere in this class.
 */
@AndroidEntryPoint
class CallifyInCallService : InCallService() {

    @Inject
    lateinit var callerRepository: CallerRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        call.registerCallback(callCallback)
        CallManager.currentCall = call

        val rawNumber = call.details?.handle?.schemeSpecificPart.orEmpty()
        if (BuildConfig.DEBUG) Log.d(TAG, "Call added — state=${call.state}, number='$rawNumber'")

        when (call.state) {
            Call.STATE_RINGING -> {
                serviceScope.launch {
                    val result = callerRepository.lookup(rawNumber)
                    val info = if (result is CallerResult.Found) result.info else null

                    CallManager.updateState(CallState.Ringing(rawNumber, info))

                    withContext(Dispatchers.Main) {
                        launchCallActivity()
                        val displayName = if (info != null)
                            "${info.firstname.orEmpty()} ${info.lastname.orEmpty()}".trim()
                        else null
                        showIncomingCallNotification(rawNumber, displayName)
                    }
                }
            }
            else -> {
                // Outgoing or already-active — surface screen immediately.
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

    private fun launchCallActivity() {
        startActivity(
            Intent(this, CallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
    }

    /**
     * Fires a heads-up / full-screen incoming call notification with
     * Accept and Decline action buttons.
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
