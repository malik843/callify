package com.callify.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telecom.Call
import android.telecom.VideoProfile
import android.util.Log
import com.callify.BuildConfig
import com.callify.service.CallManager

/**
 * BroadcastReceiver for incoming call notification action buttons.
 *
 * Handles two actions:
 * - [ACTION_ACCEPT]  — answers the ringing call (audio only)
 * - [ACTION_DECLINE] — rejects a ringing call, or disconnects an active one
 *
 * Delegates directly to [CallManager.currentCall].
 * No call history is written here or anywhere in this receiver.
 */
class CallActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val call = CallManager.currentCall
        if (call == null) {
            if (BuildConfig.DEBUG) Log.w(TAG, "onReceive: no active call in CallManager")
            return
        }

        when (intent.action) {
            ACTION_ACCEPT -> {
                if (call.state == Call.STATE_RINGING) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Accept tapped — answering call")
                    call.answer(VideoProfile.STATE_AUDIO_ONLY)
                }
            }
            ACTION_DECLINE -> {
                if (call.state == Call.STATE_RINGING) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Decline tapped — rejecting call")
                    call.reject(false, null)
                } else {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Decline tapped — disconnecting call")
                    call.disconnect()
                }
            }
        }
    }

    companion object {
        private const val TAG = "Callify"

        /** Broadcast action: answer the ringing call. */
        const val ACTION_ACCEPT  = "com.callify.ACTION_ACCEPT"

        /** Broadcast action: reject or disconnect the call. */
        const val ACTION_DECLINE = "com.callify.ACTION_DECLINE"
    }
}
