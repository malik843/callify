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
 * - [ACTION_ACCEPT]  — answers the ringing call (audio only)
 * - [ACTION_DECLINE] — rejects ringing call, or disconnects an active one
 *
 * No call history is written here.
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
                    if (BuildConfig.DEBUG) Log.d(TAG, "Accept tapped")
                    call.answer(VideoProfile.STATE_AUDIO_ONLY)
                }
            }
            ACTION_DECLINE -> {
                if (call.state == Call.STATE_RINGING) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Decline tapped")
                    call.reject(false, null)
                } else {
                    call.disconnect()
                }
            }
        }
    }

    companion object {
        private const val TAG = "Callify"
        const val ACTION_ACCEPT  = "com.callify.ACTION_ACCEPT"
        const val ACTION_DECLINE = "com.callify.ACTION_DECLINE"
    }
}
