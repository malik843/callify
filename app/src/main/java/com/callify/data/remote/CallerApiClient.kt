package com.callify.data.remote

import com.callify.data.model.CallerInfo
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit interface for the Callify backend API.
 * Handles the lookup of caller information by phone number.
 */
interface CallerApiClient {

    /**
     * ─────────────────────────────────────────────
     * CALLIFY API CALL
     * ─────────────────────────────────────────────
     * Endpoint : GET {CALLIFY_API_BASE_URL}/lookup/{phone}
     * Trigger  : CALL_STATE_RINGING — fires once per incoming call
     * Path param: phone — pre-normalised phone number string
     * Response : { "phone_number": String?,
     *              "name":         String?,
     *              "address":      String? }
     * 404      : CallerResult.NotFound
     * Timeout  : CallerResult.Timeout  (connect 3 s / read 4 s)
     * Error    : CallerResult.NetworkError
     * Cache    : LRU 50 — checked before firing; stored on 200 OK
     *
     * Number formats accepted by the API:
     *   09056226824      local with leading zero
     *   9056226824       local without leading zero
     *   +2349056226824   E.164 with country code
     * ─────────────────────────────────────────────
     */
    @GET("lookup/{phone}")
    suspend fun lookup(@Path("phone") phone: String): Response<CallerInfo>
}
