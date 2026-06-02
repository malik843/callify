package com.callify.data.remote

import com.callify.data.model.CallerInfo
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for the Callify backend API.
 * Handles the lookup of caller information by phone number.
 */
/*
 * ── MOCK MODE: API client disabled ───────────────────────────────
 * Uncomment this interface when the real API is ready.
 * See MockCallerDataSource for the active lookup path.
 * ─────────────────────────────────────────────────────────────────

interface CallerApiClient {
    /**
     * ─────────────────────────────────────────────
     * CALLIFY API CALL
     * ─────────────────────────────────────────────
     * Endpoint : POST {CALLIFY_API_BASE_URL}/lookup
     * Trigger  : CALL_STATE_RINGING — fires once per incoming call
     * Request  : { "phone": "<normalised_number>" }
     * Response : { "firstname": String?,
     *              "lastname":  String?,
     *              "address":   String? }
     * 404      : CallerResult.NotFound
     * Timeout  : CallerResult.Timeout  (connect 3s / read 4s)
     * Error    : CallerResult.NetworkError
     * Cache    : LRU 50 — checked before firing; stored on 200 OK
     * ─────────────────────────────────────────────
     */
    @POST("lookup")
    suspend fun lookup(@Body request: LookupRequest): Response<CallerInfo>
}

/**
 * Request body sent to POST /lookup.
 * The [phone] field must be pre-normalised by [PhoneNumberNormalizer]
 * before being passed here — raw device strings may contain spaces,
 * dashes, or missing country codes.
 */
data class LookupRequest(val phone: String)

 * ─────────────────────────────────────────────────────────────────
 */
