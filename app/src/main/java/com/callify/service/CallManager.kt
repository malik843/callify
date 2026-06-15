package com.callify.service

import android.telecom.Call
import com.callify.data.model.CallerInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton bridge between [CallifyInCallService] and the UI layer ([com.callify.ui.CallActivity]).
 *
 * Holds the single active [Call] reference and exposes a [StateFlow] of [CallState] so
 * [com.callify.ui.CallActivity] can observe state changes reactively without polling.
 *
 * No call history is stored here. The reference is cleared the moment
 * [CallifyInCallService.onCallRemoved] fires.
 */
object CallManager {

    /** The currently active Call object. Null when no call is in progress. */
    @Volatile
    var currentCall: Call? = null

    /** Backing state — only mutated from [updateState] and [clear]. */
    private val _callState = MutableStateFlow<CallState>(CallState.Idle)

    /**
     * Reactive call state for UI observation.
     * Collected inside [com.callify.ui.CallActivity] via [repeatOnLifecycle].
     */
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    /**
     * Pushes a new [CallState] to all observers.
     * Called from [CallifyInCallService] on the main thread.
     */
    fun updateState(state: CallState) {
        _callState.value = state
    }

    /**
     * Clears the active call reference and resets state to [CallState.Idle].
     * Called from [CallifyInCallService.onCallRemoved].
     */
    fun clear() {
        currentCall = null
        _callState.value = CallState.Idle
    }
}

/**
 * Sealed class representing the UI-visible states of an active call.
 * Consumed by [com.callify.ui.CallActivity].
 */
sealed class CallState {
    /** No call in progress. [com.callify.ui.CallActivity] finishes on this state. */
    object Idle : CallState()

    /**
     * An incoming call is ringing.
     * @param number  The raw number as delivered by [android.telecom.Call.Details].
     * @param callerInfo Resolved caller identity from the API, or null if not found.
     */
    data class Ringing(val number: String, val callerInfo: CallerInfo?) : CallState()

    /** The call has been answered and is now active. */
    object Active : CallState()

    /** The call has been disconnected (ended, missed, or declined). */
    object Disconnected : CallState()
}
