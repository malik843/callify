package com.callify.service

import android.telecom.Call
import com.callify.data.model.CallerInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton bridge between [CallifyInCallService] and [com.callify.ui.CallActivity].
 *
 * Holds the single active [Call] reference and exposes a [StateFlow] of [CallState]
 * so [com.callify.ui.CallActivity] can observe state changes reactively.
 *
 * No call history is stored here. The reference is cleared on [onCallRemoved].
 */
object CallManager {

    /** The currently active Call object. Null when no call is in progress. */
    @Volatile
    var currentCall: Call? = null

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)

    /** Reactive call state consumed by [com.callify.ui.CallActivity]. */
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    /** Push a new state to all observers. */
    fun updateState(state: CallState) { _callState.value = state }

    /** Clears the active call and resets to [CallState.Idle]. */
    fun clear() {
        currentCall = null
        _callState.value = CallState.Idle
    }
}

/**
 * Sealed class representing the UI-visible states of an active call.
 */
sealed class CallState {
    object Idle : CallState()

    /**
     * @param number     Raw number from [android.telecom.Call.Details].
     * @param callerInfo Resolved identity from the local DB, or null if not found.
     */
    data class Ringing(val number: String, val callerInfo: CallerInfo?) : CallState()

    object Active : CallState()
    object Disconnected : CallState()
}
