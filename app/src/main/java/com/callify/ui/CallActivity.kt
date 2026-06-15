package com.callify.ui

import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.telecom.VideoProfile
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.callify.databinding.ActivityCallBinding
import com.callify.service.CallManager
import com.callify.service.CallState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen call screen for both incoming and outgoing calls.
 *
 * Observes [CallManager.callState] and reacts to every state transition:
 * - [CallState.Ringing]      → shows caller name/address + Accept + Decline buttons
 * - [CallState.Active]       → shows Hang Up button only
 * - [CallState.Disconnected] → "Call ended" then finishes after 1.5 s
 * - [CallState.Idle]         → finishes immediately
 *
 * Surfaces over the lock screen via [showWhenLocked] / [turnScreenOn].
 * No call history is written when the activity finishes.
 */
@AndroidEntryPoint
class CallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Surface over the lock screen on API 27+ (manifest attrs handle older).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeCallState()
        wireButtons()
    }

    /** Collects [CallManager.callState] and updates the UI for each transition. */
    private fun observeCallState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                CallManager.callState.collect { state ->
                    when (state) {
                        is CallState.Ringing      -> renderRinging(state)
                        is CallState.Active       -> renderActive()
                        is CallState.Disconnected -> {
                            renderDisconnected()
                            delay(1_500)
                            finish()
                        }
                        is CallState.Idle -> finish()
                    }
                }
            }
        }
    }

    /** Renders the ringing state — shows caller info + Accept / Decline. */
    private fun renderRinging(state: CallState.Ringing) {
        val name    = state.callerInfo?.name?.takeIf { it.isNotBlank() } ?: "Unknown Caller"
        val address = state.callerInfo?.address.orEmpty()
        val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

        binding.avatarInitial.text    = initial
        binding.tvCallerName.text     = name
        binding.tvCallerAddress.text  = address
        binding.tvCallerAddress.visibility = if (address.isBlank()) View.GONE else View.VISIBLE
        binding.tvCallStatus.text     = "Incoming Call"

        binding.btnAccept.visibility  = View.VISIBLE
        binding.btnDecline.visibility = View.VISIBLE
        binding.btnHangUp.visibility  = View.GONE
    }

    /** Renders the active state — shows Hang Up only. */
    private fun renderActive() {
        binding.tvCallStatus.text     = "Connected"
        binding.btnAccept.visibility  = View.GONE
        binding.btnDecline.visibility = View.GONE
        binding.btnHangUp.visibility  = View.VISIBLE
    }

    /** Renders the disconnected state — brief "Call ended" message before finish. */
    private fun renderDisconnected() {
        binding.tvCallStatus.text     = "Call Ended"
        binding.btnAccept.visibility  = View.GONE
        binding.btnDecline.visibility = View.GONE
        binding.btnHangUp.visibility  = View.GONE
    }

    /** Wires Accept, Decline, and Hang-Up buttons to [CallManager.currentCall]. */
    private fun wireButtons() {
        binding.btnAccept.setOnClickListener {
            val call = CallManager.currentCall ?: return@setOnClickListener
            if (call.state == Call.STATE_RINGING) {
                call.answer(VideoProfile.STATE_AUDIO_ONLY)
            }
        }
        binding.btnDecline.setOnClickListener {
            val call = CallManager.currentCall ?: return@setOnClickListener
            call.reject(false, null)
        }
        binding.btnHangUp.setOnClickListener {
            val call = CallManager.currentCall ?: return@setOnClickListener
            call.disconnect()
        }
    }

    /**
     * Swallows the back button while a call is active so the user cannot
     * accidentally navigate away from the call screen.
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (CallManager.callState.value is CallState.Active) {
            // Intentionally blocked during active call.
            return
        }
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }
}
