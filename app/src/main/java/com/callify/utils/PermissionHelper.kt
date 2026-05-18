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
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
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
}
