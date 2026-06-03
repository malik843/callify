package com.callify

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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

    /** Launcher for the system RoleManager dialog (call-screening role). */
    private lateinit var roleRequestLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply underlines programmatically to headers
        binding.aboutLabel.paintFlags = binding.aboutLabel.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.permissionsLabel.paintFlags = binding.permissionsLabel.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        // Register the role-request launcher before any prompt can fire.
        roleRequestLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            // Result arrives when the user dismisses the role dialog.
            // Refresh the UI so the screening row reflects the new state.
            refreshPermissionStates()
            startCallifyService()
        }

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
                    arrayOf(
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.READ_CALL_LOG
                    ),
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
        // 1. Check READ_PHONE_STATE & READ_CALL_LOG
        if (!PermissionHelper.hasPhoneStatePermission(this)) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_CALL_LOG
                ),
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

        // 4. Request call-screening role (Android 10+ / API 29+)
        //    This lets CallifyScreeningService fire for every incoming call
        //    so we can capture the caller's number before RINGING is raised.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestCallScreeningRole()
        }
    }

    /**
     * Asks the system to grant Callify the [RoleManager.ROLE_CALL_SCREENING] role.
     * This is required on API 29+ for [com.callify.service.CallifyScreeningService]
     * to be invoked by the OS for every incoming call, giving us the caller number.
     *
     * The request is silently skipped if Callify already holds the role.
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun requestCallScreeningRole() {
        val roleManager = getSystemService(RoleManager::class.java) ?: return
        if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            Log.d(TAG, "Call screening role already held")
            return
        }
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            Log.w(TAG, "Call screening role not available on this device")
            return
        }
        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
        roleRequestLauncher.launch(intent)
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
        private const val TAG = "Callify"
        private const val REQUEST_CODE_PHONE_STATE = 1001
        private const val REQUEST_CODE_NOTIFICATION = 1002
    }
}
