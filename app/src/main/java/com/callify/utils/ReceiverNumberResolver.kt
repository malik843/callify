package com.callify.utils

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log
import com.callify.BuildConfig

/**
 * Resolves the device's own phone number (the receiver field
 * in the call log) from the SIM via TelephonyManager.
 *
 * Returns the normalised 10-digit number if available,
 * or "unknown_receiver" if the carrier does not provision it
 * or permission is missing.
 *
 * This value is read per-call at the moment of logging —
 * it is not cached globally because SIM state can change
 * (dual SIM, SIM swap).
 */
object ReceiverNumberResolver {

    @SuppressLint("MissingPermission", "HardwareIds")
    fun resolve(context: Context): String {
        if (!PermissionHelper.hasPhoneNumbersPermission(context)) {
            return "unknown_receiver"
        }
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val raw = tm.line1Number
            PhoneNumberNormalizer.normalizeForLocalLookup(raw)
                ?: "unknown_receiver"
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e("Callify", "ReceiverNumberResolver error: ${e.message}")
            "unknown_receiver"
        }
    }
}
