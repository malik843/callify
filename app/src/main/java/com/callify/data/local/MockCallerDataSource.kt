package com.callify.data.local

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
 * ─────────────────────────────────────────────────────────────────
 */
class MockCallerDataSource(private val dao: CallerDao) {

    /**
     * Looks up a caller by normalised phone number.
     * Returns Found or NotFound — no network states possible
     * from a local database, so Timeout and NetworkError are
     * not returned here. CallerRepository maps the two-state
     * result into the full CallerResult sealed class.
     */
    suspend fun lookup(normalizedPhone: String): CallerInfo? {
        return withContext(Dispatchers.IO) {
            dao.findByPhone(normalizedPhone)
        }
    }
}
