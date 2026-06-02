package com.callify

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.callify.databinding.ActivityMainBinding
import com.callify.service.CallDetectorService
import com.callify.utils.PermissionHelper

/**
 * The main entry point of the Callify application.
 *
 * This activity displays the Permission Dashboard UI, allowing users to view
 * the current status of the runtime permissions required for the app's functionality
 * (Phone State, Notifications, and Draw Over Apps/Overlay).
 *
 * On initial launch, the activity prompts the user for permissions sequentially.
 * If any permissions are denied or revoked, the UI reflects their states, and the user
 * can tap the rows to request them manually.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var hasPromptedOverlayThisSession = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply underlines programmatically to headers
        binding.aboutLabel.paintFlags = binding.aboutLabel.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.permissionsLabel.paintFlags = binding.permissionsLabel.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        wirePermissionRowClicks()
        refreshPermissionStates()
        autoPromptNextPermission()
        startCallifyService()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStates()
        autoPromptNextPermission()
        startCallifyService()
    }

    /**
     * Reads the current grant state of each runtime permission
     * and updates the SwitchCompat toggles to reflect reality.
     * Called on onCreate() and onResume() so the UI always
     * reflects the true system state.
     */
    private fun refreshPermissionStates() {
        binding.switchPhoneState.isChecked =
            PermissionHelper.hasPhoneStatePermission(this)

        binding.switchNotifications.isChecked =
            PermissionHelper.hasNotificationPermission(this)

        binding.switchOverlay.isChecked =
            PermissionHelper.hasOverlayPermission(this)
    }

    /**
     * Wires each permission row's click listener.
     * Tapping a row that is already granted does nothing.
     * Tapping a denied row routes to the appropriate
     * permission request or system settings screen.
     */
    private fun wirePermissionRowClicks() {
        binding.rowPhoneState.setOnClickListener {
            if (!PermissionHelper.hasPhoneStatePermission(this)) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_PHONE_STATE),
                    REQUEST_CODE_PHONE_STATE
                )
            }
        }

        binding.rowNotifications.setOnClickListener {
            if (!PermissionHelper.hasNotificationPermission(this)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        REQUEST_CODE_NOTIFICATION
                    )
                }
            }
        }

        binding.rowOverlay.setOnClickListener {
            if (!PermissionHelper.hasOverlayPermission(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
    }

    /**
     * Sequentially auto-prompts the user for missing permissions on launch.
     * Uses a session boolean flag to prevent redirect loops for overlay permission.
     */
    private fun autoPromptNextPermission() {
        // 1. Check READ_PHONE_STATE
        if (!PermissionHelper.hasPhoneStatePermission(this)) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                REQUEST_CODE_PHONE_STATE
            )
            return
        }

        // 2. Check SYSTEM_ALERT_WINDOW (Overlay)
        if (!PermissionHelper.hasOverlayPermission(this)) {
            if (!hasPromptedOverlayThisSession) {
                hasPromptedOverlayThisSession = true
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
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
    }

    /**
     * Starts the foreground service if and only if all required runtime
     * permissions are granted.
     */
    private fun startCallifyService() {
        if (PermissionHelper.hasPhoneStatePermission(this) &&
            PermissionHelper.hasNotificationPermission(this) &&
            PermissionHelper.hasOverlayPermission(this)) {
            val intent = Intent(this, CallDetectorService::class.java)
            ContextCompat.startForegroundService(this, intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Refresh all switches regardless of which permission was just
        // acted on — simplest and most correct approach
        refreshPermissionStates()

        // If the permission was granted, prompt the next one automatically
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            autoPromptNextPermission()
        }

        // If all permissions now granted, start the service
        startCallifyService()
    }

    companion object {
        private const val REQUEST_CODE_PHONE_STATE = 1001
        private const val REQUEST_CODE_NOTIFICATION = 1002
    }
}
