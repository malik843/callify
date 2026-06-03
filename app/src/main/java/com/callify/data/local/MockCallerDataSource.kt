package com.callify.data.local

import android.util.Log
import com.callify.data.model.CallerInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Mock data source used while the Callify API is in development.
 *
 * ─────────────────────────────────────────────────────────────────
 * MOCK ACTIVE — real API call commented out
 * ─────────────────────────────────────────────────────────────────
 * When the API is ready:
 *   1. Delete this file
 *   2. Uncomment CallerApiClient usage in CallerRepository
 *   3. Uncomment NetworkModule in the DI graph
 *   4. Remove Room dependency if no longer needed
 *   5. Remove logging helper
 * ─────────────────────────────────────────────────────────────────
 */
class MockCallerDataSource(private val dao: CallerDao) {
    private val TAG = "Callify"

    /**
     * Looks up a caller by normalised phone number.
     * Returns Found or NotFound — no network states possible
     * from a local database, so Timeout and NetworkError are
     * not returned here. CallerRepository maps the two-state
     * result into the full CallerResult sealed class.
     */
    suspend fun lookup(normalizedPhone: String): CallerInfo? {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "MockCallerDataSource: looking up phone number: '$normalizedPhone'")
            val result = dao.findByPhone(normalizedPhone)
            if (result != null) {
                Log.d(TAG, "MockCallerDataSource: Found caller in DB: ${result.firstname} ${result.lastname}")
            } else {
                Log.d(TAG, "MockCallerDataSource: No caller found in DB for: '$normalizedPhone'")
            }
            result
        }
    }
}
