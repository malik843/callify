package com.callify.utils

/**
 * Utility for normalizing phone numbers to a consistent format.
 */
object PhoneNumberNormalizer {

    /**
     * Strips all non-digit characters except a leading '+'.
     * Returns the cleaned number, or null if the result is empty.
     *
     * @param raw The raw phone number string from the system.
     * @return Normalized phone number or null.
     */
    fun normalize(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        
        // Strip spaces, dashes, parentheses, and dots
        val stripped = raw.trim()
            .replace(Regex("[\\s\\-().]+"), "")
            
        return if (stripped.isEmpty()) null else stripped
    }

    /**
     * Strips country code and leading zero so the result
     * matches the 10-digit format stored in the local database.
     *
     * Examples:
     *   +2349074086115  →  9074086115
     *   2349074086115   →  9074086115
     *   09074086115     →  9074086115
     *   9074086115      →  9074086115  (already correct)
     *
     * Extend this function when the real API is integrated
     * if the server expects a different format.
     */
    fun normalizeForLocalLookup(raw: String?): String? {
        val base = normalize(raw) ?: return null
        return when {
            base.startsWith("+234") -> base.removePrefix("+234")
            base.startsWith("234")  -> base.removePrefix("234")
            base.startsWith("0")    -> base.removePrefix("0")
            else                    -> base
        }
    }
}
