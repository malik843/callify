package com.callify.repository

import com.callify.data.model.CallerInfo
import com.callify.data.model.CallerResult
import com.callify.data.remote.CallerApiClient
import com.callify.data.remote.LookupRequest
import com.callify.utils.PhoneNumberNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for coordinating caller data lookups.
 * Acts as the single source of truth for caller information, 
 * handling API requests and in-memory caching.
 */
@Singleton
class CallerRepository @Inject constructor(
    private val apiClient: CallerApiClient,
    private val callerCache: MutableMap<String, CallerInfo>
) {

    /**
     * Look up caller information for the given phone number.
     * 
     * Logic:
     * 1. Normalize the phone number.
     * 2. Check the LRU cache first using the normalized key.
     * 3. If miss, fire API request.
     * 4. Handle success, 404, other HTTP errors, timeouts, and IO failures.
     * 5. Update cache on successful lookup.
     *
     * @param phoneNumber The raw phone number string to identify.
     * @return A [CallerResult] representing the outcome of the lookup.
     */
    suspend fun lookup(phoneNumber: String): CallerResult = withContext(Dispatchers.IO) {
        // 1. Normalize the number
        val key = PhoneNumberNormalizer.normalize(phoneNumber) 
            ?: return@withContext CallerResult.NotFound

        // 2. Check LRU Cache
        callerCache[key]?.let {
            return@withContext CallerResult.Found(it)
        }

        // 3. Fire Network Request
        return@withContext try {
            val response = apiClient.lookup(LookupRequest(key))

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    // 4. Update Cache & Return Found
                    callerCache[key] = body
                    CallerResult.Found(body)
                } else {
                    CallerResult.NetworkError
                }
            } else {
                // 5. Handle HTTP Errors
                when (response.code()) {
                    404 -> CallerResult.NotFound
                    else -> CallerResult.NetworkError
                }
            }
        } catch (e: SocketTimeoutException) {
            // 6. Handle Timeout
            CallerResult.Timeout
        } catch (e: IOException) {
            // 7. Handle other Network Errors
            CallerResult.NetworkError
        } catch (e: Exception) {
            CallerResult.NetworkError
        }
    }
}
