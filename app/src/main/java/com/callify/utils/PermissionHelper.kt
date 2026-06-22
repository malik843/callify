package com.callify.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Utility for checking and requesting required permissions at runtime.
 */
object PermissionHelper {

    /**
     * Checks if the READ_PHONE_STATE permission is granted.
     *
     * @param context The application context.
     * @return True if permission is granted, false otherwise.
     */
    fun hasPhoneStatePermission(context: Context): Boolean {
        val phoneStateGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val callLogGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        return phoneStateGranted && callLogGranted
    }

    /**
     * Checks if the SYSTEM_ALERT_WINDOW (overlay) permission is granted.
     *
     * @param context The application context.
     * @return True if permission is granted, false otherwise.
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Returns true if POST_NOTIFICATIONS permission is granted,
     * or if the device is below API 33 (permission did not exist).
     *
     * @param context The application context.
     * @return True if permission is granted or not required.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required below API 33
        }
    }

    /**
     * Returns true if READ_PHONE_NUMBERS is granted.
     * Required on API 26+ to read TelephonyManager.getLine1Number()
     * (the device's own SIM number used for the call log receiver field).
     * On API 33+ this is also enforced at runtime; this guard handles both cleanly.
     */
    fun hasPhoneNumbersPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_NUMBERS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
