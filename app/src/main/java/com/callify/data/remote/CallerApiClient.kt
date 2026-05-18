package com.callify.data.remote

import com.callify.data.model.CallerInfo
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for the Callify backend API.
 * Handles the lookup of caller information by phone number.
 */
interface CallerApiClient {
    /**
     * Sends a lookup request to the backend to identify a caller.
     *
     * @param request The [LookupRequest] containing the phone number.
     * @return A [Response] containing [CallerInfo] if successful.
     */
    @POST("lookup")
    suspend fun lookup(@Body request: LookupRequest): Response<CallerInfo>
}

/**
 * Request body for the caller lookup API.
 *
 * @property phone The phone number to identify.
 */
data class LookupRequest(val phone: String)
