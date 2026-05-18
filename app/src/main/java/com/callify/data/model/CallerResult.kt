package com.callify.data.model

/**
 * Sealed class representing the possible outcomes of a caller lookup operation.
 */
sealed class CallerResult {
    /**
     * Successfully found caller information.
     * @property info The retrieved [CallerInfo].
     */
    data class Found(val info: CallerInfo) : CallerResult()

    /**
     * No record was found for the given phone number (404).
     */
    object NotFound : CallerResult()

    /**
     * A network error occurred during the lookup (5xx, connection failure, etc.).
     */
    object NetworkError : CallerResult()

    /**
     * The lookup request timed out based on defined OkHttp timeouts.
     */
    object Timeout : CallerResult()
}
