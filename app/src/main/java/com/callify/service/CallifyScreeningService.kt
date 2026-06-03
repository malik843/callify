package com.callify.service

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi
import com.callify.BuildConfig

/**
 * Called by the OS **before** an incoming call is presented to the user.
 *
 * Android 29+ delivers the incoming call handle here rather than in
 * [android.telephony.TelephonyCallback] or [android.telephony.PhoneStateListener].
 * We store the E.164/local number in [pendingNumber] so that
 * [CallDetectorService] can read it when [android.telephony.TelephonyManager.CALL_STATE_RINGING]
 * fires a moment later.
 *
 * The service always responds with `allow = true` — Callify is an ID app,
 * not a call-blocker.
 *
 * Registration requirements:
 *  - `<service>` entry in AndroidManifest with
 *    `android.telecom.CallScreeningService` intent-filter + BIND_SCREENING_SERVICE permission
 *  - User must grant the role via [android.app.role.RoleManager.ROLE_CALL_SCREENING]
 *    (prompted in MainActivity on first run)
 */
@RequiresApi(Build.VERSION_CODES.Q)
class CallifyScreeningService : CallScreeningService() {

    companion object {
        private const val TAG = "Callify"

        /**
         * The last incoming number seen by this service.
         * Written by [CallifyScreeningService] on the binder thread before
         * CALL_STATE_RINGING fires; read by [CallDetectorService] on the main
         * thread. Marked volatile for cross-thread visibility.
         */
        @Volatile
        var pendingNumber: String? = null
            private set

        /** Called by [CallDetectorService] after consuming the number. */
        fun clearPendingNumber() {
            pendingNumber = null
        }
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val handle = callDetails.handle          // e.g. tel:+639056226824
        val raw    = handle?.schemeSpecificPart  // strips "tel:" prefix

        pendingNumber = raw
        if (BuildConfig.DEBUG) Log.d(TAG, "CallScreeningService: raw='$raw'")

        // Always allow — Callify is an identifier, not a blocker.
        respondToCall(
            callDetails,
            CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSilenceCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
        )
    }
}
