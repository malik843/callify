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
 * - [CallState.Ringing]      → caller info + Accept + Decline
 * - [CallState.Active]       → Hang Up button only
 * - [CallState.Disconnected] → "Call ended" then auto-finish after 1.5 s
 * - [CallState.Idle]         → finish immediately
 *
 * Back button is swallowed during [CallState.Active] to prevent accidental
 * navigation away from an in-progress call.
 */
@AndroidEntryPoint
class CallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        observeCallState()
        wireButtons()
    }

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

    private fun renderRinging(state: CallState.Ringing) {
        val name = state.callerInfo?.let {
            "${it.firstname.orEmpty()} ${it.lastname.orEmpty()}".trim()
        }?.takeIf { it.isNotBlank() } ?: "Unknown Caller"
        val address = state.callerInfo?.address.orEmpty()

        binding.avatarInitial.text    = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        binding.tvCallerName.text     = name
        binding.tvCallerAddress.text  = address
        binding.tvCallerAddress.visibility = if (address.isBlank()) View.GONE else View.VISIBLE
        binding.tvCallStatus.text     = "Incoming Call"
        binding.btnAccept.visibility  = View.VISIBLE
        binding.btnDecline.visibility = View.VISIBLE
        binding.btnHangUp.visibility  = View.GONE
    }

    private fun renderActive() {
        binding.tvCallStatus.text     = "Connected"
        binding.btnAccept.visibility  = View.GONE
        binding.btnDecline.visibility = View.GONE
        binding.btnHangUp.visibility  = View.VISIBLE
    }

    private fun renderDisconnected() {
        binding.tvCallStatus.text     = "Call Ended"
        binding.btnAccept.visibility  = View.GONE
        binding.btnDecline.visibility = View.GONE
        binding.btnHangUp.visibility  = View.GONE
    }

    private fun wireButtons() {
        binding.btnAccept.setOnClickListener {
            CallManager.currentCall?.takeIf { it.state == Call.STATE_RINGING }
                ?.answer(VideoProfile.STATE_AUDIO_ONLY)
        }
        binding.btnDecline.setOnClickListener {
            CallManager.currentCall?.reject(false, null)
        }
        binding.btnHangUp.setOnClickListener {
            CallManager.currentCall?.disconnect()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (CallManager.callState.value is CallState.Active) return
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }
}
