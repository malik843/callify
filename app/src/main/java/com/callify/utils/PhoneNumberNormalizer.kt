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
}
