package com.callify.data.model

/**
 * Data class representing the information about a caller.
 *
 * Deserialised directly from the JSON body returned by GET /lookup/{phone}.
 *
 * API response shape:
 * {
 *   "phone_number": "9056226824",
 *   "name":         "Malik Yusuff",
 *   "address":      "plot 16, otungba jobi fele way, ikeja."
 * }
 *
 * @property phone_number The phone number as stored server-side.
 * @property name         The full name of the caller (first + last combined).
 * @property address      The physical address of the caller.
 */
data class CallerInfo(
    val phone_number: String?,
    val name:         String?,
    val address:      String?
)
