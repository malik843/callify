package com.callify.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data class representing the information about a caller.
 *
 * @property firstname The first name of the caller.
 * @property lastname The last name of the caller.
 * @property phone The phone number of the caller.
 * @property address The physical address of the caller.
 */
@Entity(tableName = "contacts")
data class CallerInfo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "first_name")
    val firstname: String?,

    @ColumnInfo(name = "last_name")
    val lastname: String?,

    @ColumnInfo(name = "phone")
    val phone: String?,

    @ColumnInfo(name = "address")
    val address: String?
)
