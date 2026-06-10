package com.callify.repository

import android.util.Log
import com.callify.BuildConfig
import com.callify.data.model.CallerInfo
import com.callify.data.model.CallerResult
import com.callify.data.remote.CallerApiClient
import com.callify.utils.PhoneNumberNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for coordinating caller data lookups.
 * Acts as the single source of truth for caller information,
 * handling API requests and in-memory LRU caching.
 */
@Singleton
class CallerRepository @Inject constructor(
    private val apiClient: CallerApiClient,
    private val callerCache: MutableMap<String, CallerInfo>
) {

    private val TAG = "Callify"

    /**
     * ─────────────────────────────────────────────
     * CALLIFY API CALL
     * ─────────────────────────────────────────────
     * Endpoint : GET {CALLIFY_API_BASE_URL}/lookup/{phone}
     * Trigger  : CALL_STATE_RINGING — fires once per incoming call
     * Path param: normalized phone number string
     * Response : { "phone_number": String?,
     *              "name":         String?,
     *              "address":      String? }
     * 404      : CallerResult.NotFound
     * Timeout  : CallerResult.Timeout  (connect 3 s / read 4 s)
     * Error    : CallerResult.NetworkError
     * Cache    : LRU 50 — checked before firing; stored on 200 OK
     *
     * Number formats accepted by the API (all three work):
     *   09056226824      local with leading zero
     *   9056226824       local without leading zero
     *   +2349056226824   E.164 with country code
     * ─────────────────────────────────────────────
     */
    suspend fun lookup(phoneNumber: String): CallerResult = withContext(Dispatchers.IO) {
        // 1. Basic normalisation — strip spaces, dashes, parens.
        //    We use normalize() (not normalizeForLocalLookup()) so the
        //    leading zero / country code is preserved for the API.
        val key = PhoneNumberNormalizer.normalize(phoneNumber)
            ?: return@withContext CallerResult.NotFound

        // 2. Cache check — hit returns immediately, no API call fired.
        callerCache[key]?.let {
            if (BuildConfig.DEBUG) Log.d(TAG, "Cache hit for $key")
            return@withContext CallerResult.Found(it)
        }

        // 3. Fire the API call — GET /lookup/{phone}
        return@withContext try {
            val response = apiClient.lookup(key)

            when {
                response.isSuccessful -> {
                    val info = response.body()
                    if (info != null) {
                        // Cache the successful result.
                        callerCache[key] = info
                        CallerResult.Found(info)
                    } else {
                        // 200 OK but empty body — treat as not found.
                        Log.w(TAG, "200 OK but null body for $key")
                        CallerResult.NotFound
                    }
                }
                response.code() == 404 -> {
                    Log.d(TAG, "404 — no record for $key")
                    CallerResult.NotFound
                }
                else -> {
                    Log.e(TAG, "API error ${response.code()} for $key")
                    CallerResult.NetworkError
                }
            }
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Lookup timed out for $key", e)
            CallerResult.Timeout
        } catch (e: Exception) {
            Log.e(TAG, "Lookup failed for $key", e)
            CallerResult.NetworkError
        }
    }
}
