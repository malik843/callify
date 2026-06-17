package com.callify

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.callify.databinding.ActivityMainBinding
import com.callify.service.CallDetectorService
import com.callify.ui.MainPagerAdapter
import com.callify.utils.PermissionHelper

/**
 * Main entry point of the Callify application.
 *
 * Hosts two tabs:
 * - Tab 0 "Dial"        → [com.callify.ui.DialPadFragment]
 * - Tab 1 "Permissions" → [com.callify.ui.PermissionsFragment]
 *
 * Also responsible for:
 * - Sequentially requesting runtime permissions on first launch
 * - Requesting the dialer role (merged launcher):
 *     1. ROLE_DIALER first → covers both dialing + incoming call handling
 *     2. If denied → ROLE_CALL_SCREENING fallback (observer mode)
 * - Starting [CallDetectorService] once all permissions are confirmed
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var hasPromptedOverlayThisSession = false

    /**
     * Unified role launcher.
     *
     * ROLE_DIALER is requested first — it covers:
     *   - Outgoing calls via the Dial tab (DialPadFragment)
     *   - Incoming call handling via CallifyInCallService
     *
     * If denied, ROLE_CALL_SCREENING is requested as a fallback so
     * CallifyScreeningService can still capture incoming numbers (observer mode).
     */
    private lateinit var roleRequestLauncher: ActivityResultLauncher<Intent>

    /** Guards against re-prompting ROLE_DIALER in the same session. */
    private var hasAttemptedDialerRole = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTabs()
        setupRoleLauncher()
        autoPromptNextPermission()
        startCallifyService()
    }

    override fun onResume() {
        super.onResume()
        autoPromptNextPermission()
        startCallifyService()
    }

    // ── Tabs ──────────────────────────────────────────────────────────────

    private fun setupTabs() {
        binding.viewPager.adapter = MainPagerAdapter(this)
        // Swipe is disabled — icon buttons are the only tab navigation.
        binding.viewPager.isUserInputEnabled = false

        binding.tabDial.setOnClickListener        { selectTab(0) }
        binding.tabPermissions.setOnClickListener { selectTab(1) }
    }

    /**
     * Switches the ViewPager to [index] and updates the icon button appearance:
     * - Selected tab: white oval background + dark icon tint
     * - Deselected tab: transparent background + grey icon tint
     */
    private fun selectTab(index: Int) {
        binding.viewPager.currentItem = index
        if (index == 0) {
            binding.tabDial.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.tabDial.setColorFilter(Color.parseColor("#1A1A1A"))
            binding.tabPermissions.setBackgroundColor(Color.TRANSPARENT)
            binding.tabPermissions.setColorFilter(Color.parseColor("#888888"))
        } else {
            binding.tabPermissions.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.tabPermissions.setColorFilter(Color.parseColor("#1A1A1A"))
            binding.tabDial.setBackgroundColor(Color.TRANSPARENT)
            binding.tabDial.setColorFilter(Color.parseColor("#888888"))
        }
    }

    // ── Role launcher ─────────────────────────────────────────────────────

    private fun setupRoleLauncher() {
        roleRequestLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                if (BuildConfig.DEBUG) Log.d(TAG, "Role granted")
            } else {
                // ROLE_DIALER denied — fall back to screening role
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestScreeningRoleFallback()
                } else {
                    Toast.makeText(
                        this, "Callify works best as the default phone app", Toast.LENGTH_LONG
                    ).show()
                }
            }
            startCallifyService()
        }
    }

    /**
     * Requests [RoleManager.ROLE_DIALER] — the primary role covering both
     * outgoing dialing and incoming call detection via CallifyInCallService.
     * Silently skipped if already held.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestDialerRole() {
        if (hasAttemptedDialerRole) return
        hasAttemptedDialerRole = true

        val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
        if (roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
            if (BuildConfig.DEBUG) Log.d(TAG, "ROLE_DIALER already held")
            return
        }
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
            if (BuildConfig.DEBUG) Log.w(TAG, "ROLE_DIALER not available — requesting screening fallback")
            requestScreeningRoleFallback()
            return
        }
        roleRequestLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
    }

    /**
     * Fallback: requests [RoleManager.ROLE_CALL_SCREENING] so
     * [com.callify.service.CallifyScreeningService] can capture incoming numbers
     * when Callify is NOT the default dialer.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestScreeningRoleFallback() {
        val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
        if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            if (BuildConfig.DEBUG) Log.d(TAG, "ROLE_CALL_SCREENING already held")
            return
        }
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            if (BuildConfig.DEBUG) Log.w(TAG, "ROLE_CALL_SCREENING not available")
            return
        }
        roleRequestLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
    }

    // ── Permissions ───────────────────────────────────────────────────────

    /**
     * Sequentially auto-prompts for missing permissions.
     * Role request fires last — only after all runtime permissions are confirmed.
     */
    private fun autoPromptNextPermission() {
        // 1. READ_PHONE_STATE + READ_CALL_LOG
        if (!PermissionHelper.hasPhoneStatePermission(this)) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.READ_PHONE_STATE,
                    android.Manifest.permission.READ_CALL_LOG
                ),
                REQUEST_CODE_PHONE_STATE
            )
            return
        }

        // 2. SYSTEM_ALERT_WINDOW (overlay)
        if (!PermissionHelper.hasOverlayPermission(this)) {
            if (!hasPromptedOverlayThisSession) {
                hasPromptedOverlayThisSession = true
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
            return
        }

        // 3. POST_NOTIFICATIONS (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionHelper.hasNotificationPermission(this)) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATION
                )
                return
            }
        }

        // 4. Dialer role — after all runtime permissions confirmed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestDialerRole()
        }
    }

    /**
     * Starts [CallDetectorService] if all required runtime permissions are granted.
     * The service operates in observer mode when Callify is not the default dialer.
     */
    private fun startCallifyService() {
        if (PermissionHelper.hasPhoneStatePermission(this) &&
            PermissionHelper.hasNotificationPermission(this) &&
            PermissionHelper.hasOverlayPermission(this)) {
            ContextCompat.startForegroundService(
                this, Intent(this, CallDetectorService::class.java)
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Refresh the Permissions tab switches
        permissionsFragment()?.refreshPermissionStates()
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            autoPromptNextPermission()
        }
        startCallifyService()
    }

    /** Returns the [PermissionsFragment] if currently attached. */
    private fun permissionsFragment() =
        supportFragmentManager.fragments
            .filterIsInstance<com.callify.ui.PermissionsFragment>()
            .firstOrNull()

    companion object {
        private const val TAG = "Callify"
        private const val REQUEST_CODE_PHONE_STATE  = 1001
        private const val REQUEST_CODE_NOTIFICATION = 1002
    }
}
