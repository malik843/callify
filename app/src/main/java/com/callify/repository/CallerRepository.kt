package com.callify.repository

import com.callify.data.local.MockCallerDataSource
import com.callify.data.model.CallerInfo
import com.callify.data.model.CallerResult
import com.callify.utils.PhoneNumberNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for coordinating caller data lookups.
 * Acts as the single source of truth for caller information, 
 * handling API requests and in-memory caching.
 */
@Singleton
class CallerRepository @Inject constructor(
    // ── MOCK MODE ─────────────────────────────────────────────────
    // When API is ready: replace mockDataSource with apiClient
    // and uncomment the network block below
    private val mockDataSource: MockCallerDataSource,
    // private val apiClient: CallerApiClient,  // ← restore for API
    // ──────────────────────────────────────────────────────────────
    private val callerCache: MutableMap<String, CallerInfo>
) {

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
    suspend fun lookup(phoneNumber: String): CallerResult = withContext(Dispatchers.IO) {
        // 1. Normalize the number
        val key = PhoneNumberNormalizer.normalizeForLocalLookup(phoneNumber) 
            ?: return@withContext CallerResult.NotFound

        // CACHE CHECK — hit returns immediately, no API call fired
        callerCache[key]?.let {
            return@withContext CallerResult.Found(it)
        }

        // ── MOCK LOOKUP (replace with API call when ready) ────────
        val result = mockDataSource.lookup(key)
        // ── END MOCK ──────────────────────────────────────────────

        // ── REAL API CALL (uncomment when API is ready) ───────────
        // val response = apiClient.lookup(LookupRequest(key))
        // if (!response.isSuccessful) return if (response.code() == 404)
        //     CallerResult.NotFound else CallerResult.NetworkError
        // val result = response.body()
        // ── END REAL API ──────────────────────────────────────────

        return@withContext if (result != null) {
            // CACHE STORE — successful response cached for future calls
            callerCache[key] = result
            CallerResult.Found(result)
        } else {
            CallerResult.NotFound
        }
    }
}
