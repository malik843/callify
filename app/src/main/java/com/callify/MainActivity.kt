package com.callify

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.callify.databinding.ActivityMainBinding
import com.callify.service.CallDetectorService
import com.callify.ui.MainPagerAdapter
import com.callify.utils.PermissionHelper
import com.google.android.material.tabs.TabLayoutMediator

/**
 * Main entry point of the Callify application.
 *
 * Hosts the two-tab layout:
 * - Tab 0 "Dial"        → [com.callify.ui.DialPadFragment]
 * - Tab 1 "Permissions" → [com.callify.ui.PermissionsFragment]
 *
 * Also responsible for:
 * - Sequentially requesting runtime permissions on first launch
 * - Requesting the dialer role (covers both default dialer + screening fallback)
 * - Starting [CallDetectorService] once all permissions are confirmed
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var hasPromptedOverlayThisSession = false

    /**
     * Unified role launcher.
     *
     * Strategy (merged as per user direction):
     * 1. First attempt: request [RoleManager.ROLE_DIALER].
     *    If granted → [CallifyInCallService] handles everything (dial + incoming).
     * 2. If denied → request [RoleManager.ROLE_CALL_SCREENING] as a fallback so
     *    [com.callify.service.CallifyScreeningService] can still capture incoming numbers
     *    in observer mode.
     */
    private lateinit var roleRequestLauncher: ActivityResultLauncher<Intent>

    /** True once ROLE_DIALER has been attempted this session (prevents re-prompting). */
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

    /** Wires [MainPagerAdapter] to [ViewPager2] and attaches [TabLayoutMediator]. */
    private fun setupTabs() {
        val adapter = MainPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0    -> "Dial"
                else -> "Permissions"
            }
        }.attach()
    }

    // ── Role requests ─────────────────────────────────────────────────────

    /**
     * Registers the launcher that handles results from both role dialogs.
     *
     * Flow:
     * - ROLE_DIALER granted  → service + dialer both active; done.
     * - ROLE_DIALER denied   → fall back and request ROLE_CALL_SCREENING.
     * - ROLE_CALL_SCREENING  → result ignored (screening service registers silently).
     */
    private fun setupRoleLauncher() {
        roleRequestLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                if (BuildConfig.DEBUG) Log.d(TAG, "Role granted")
            } else {
                // ROLE_DIALER denied — fall back to screening role.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestScreeningRoleFallback()
                } else {
                    Toast.makeText(
                        this,
                        "Callify works best as the default phone app",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            startCallifyService()
        }
    }

    /**
     * Requests [RoleManager.ROLE_DIALER] — the primary role that covers both
     * outgoing dialing and incoming call detection via [CallifyInCallService].
     *
     * Called after all runtime permissions are confirmed.
     * Silently skipped if the role is already held or not available.
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
        roleRequestLauncher.launch(
            roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
        )
    }

    /**
     * Fallback: requests [RoleManager.ROLE_CALL_SCREENING] so
     * [com.callify.service.CallifyScreeningService] can capture incoming numbers
     * when Callify is NOT the default dialer.
     *
     * Silently skipped if already held or not available.
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
        roleRequestLauncher.launch(
            roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
        )
    }

    // ── Permissions ───────────────────────────────────────────────────────

    /**
     * Sequentially auto-prompts the user for missing permissions on launch.
     * Role request fires last — only after all runtime permissions are confirmed.
     */
    private fun autoPromptNextPermission() {
        // 1. READ_PHONE_STATE + READ_CALL_LOG
        if (!PermissionHelper.hasPhoneStatePermission(this)) {
            requestPermissions(
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
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:$packageName")
                    )
                )
            }
            return
        }

        // 3. POST_NOTIFICATIONS (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionHelper.hasNotificationPermission(this)) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATION
                )
                return
            }
        }

        // 4. Dialer role — fires AFTER all runtime permissions are confirmed.
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
                this,
                Intent(this, CallDetectorService::class.java)
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Refresh the Permissions tab switches.
        permissionsFragment()?.refreshPermissionStates()

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            autoPromptNextPermission()
        }
        startCallifyService()
    }

    /** Returns the [PermissionsFragment] instance if it is currently attached. */
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
