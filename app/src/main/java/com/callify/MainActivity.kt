package com.callify

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.callify.service.CallDetectorService
import com.callify.utils.PermissionHelper

/**
 * The main entry point of the Callify application.
 *
 * This activity is responsible for:
 * 1. Requesting the required READ_PHONE_STATE permission.
 * 2. Requesting the SYSTEM_ALERT_WINDOW (overlay) permission via system settings.
 * 3. Requesting POST_NOTIFICATIONS on Android 13+.
 * 4. Starting the [CallDetectorService] once all necessary permissions are granted.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_NOTIFICATION = 1002
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCallifyService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startCallifyService()
    }

    override fun onResume() {
        super.onResume()
        // Re-check and start if user just granted permission in settings
        startCallifyService()
    }

    /**
     * Orchestrates the permission check and service start flow.
     */
    private fun startCallifyService() {
        // 1. Check READ_PHONE_STATE
        if (!PermissionHelper.hasPhoneStatePermission(this)) {
            requestPermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
            return
        }

        // 2. Check SYSTEM_ALERT_WINDOW
        if (!PermissionHelper.hasOverlayPermission(this)) {
            checkAndRequestOverlayPermission()
            return
        }

        // 3. Check POST_NOTIFICATIONS (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionHelper.hasNotificationPermission(this)) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATION
                )
                return
            }
        }

        // 4. Start Service
        val intent = Intent(this, CallDetectorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    /**
     * Checks for SYSTEM_ALERT_WINDOW permission and opens system settings if missing.
     */
    private fun checkAndRequestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                getString(R.string.overlay_permission_explanation),
                Toast.LENGTH_LONG
            ).show()

            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_NOTIFICATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCallifyService()
            } else {
                Toast.makeText(
                    this,
                    "Callify needs notification permission to stay active during calls",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
