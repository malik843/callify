package com.example.callify

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
import androidx.core.content.ContextCompat
import com.example.callify.service.CallDetectorService

/**
 * The main entry point of the Callify application.
 *
 * This activity is responsible for:
 * 1. Requesting the required READ_PHONE_STATE permission.
 * 2. Requesting the SYSTEM_ALERT_WINDOW (overlay) permission via system settings.
 * 3. Starting the [CallDetectorService] once all necessary permissions are granted.
 */
class MainActivity : AppCompatActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            checkAndRequestOverlayPermission()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestPhoneStatePermission()
    }

    override fun onResume() {
        super.onResume()
        // Verify permissions again when returning to the activity
        if (hasAllPermissions()) {
            startCallDetectorService()
        }
    }

    /**
     * Checks for READ_PHONE_STATE permission and requests it if missing.
     */
    private fun checkAndRequestPhoneStatePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            checkAndRequestOverlayPermission()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
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
        } else if (hasAllPermissions()) {
            startCallDetectorService()
        }
    }

    /**
     * Checks if all required permissions have been granted.
     *
     * @return True if both READ_PHONE_STATE and SYSTEM_ALERT_WINDOW are granted.
     */
    private fun hasAllPermissions(): Boolean {
        val hasPhoneState = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        val hasOverlay = Settings.canDrawOverlays(this)
        return hasPhoneState && hasOverlay
    }

    /**
     * Starts the [CallDetectorService] to begin listening for incoming calls.
     */
    private fun startCallDetectorService() {
        val intent = Intent(this, CallDetectorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
