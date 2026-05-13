package com.example.callify.data.model

/**
 * Data class representing the information about a caller.
 *
 * @property firstname The first name of the caller.
 * @property lastname The last name of the caller.
 * @property phone The phone number of the caller.
 * @property address The physical address of the caller.
 */
data class CallerInfo(
    val firstname: String?,
    val lastname: String?,
    val phone: String?,
    val address: String?
)
