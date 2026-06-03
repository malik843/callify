package com.callify.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.callify.data.model.CallerInfo

@Dao
interface CallerDao {

    /**
     * Looks up a contact by phone number.
     * Returns the first match or null if not found.
     * Called by MockCallerDataSource — not called directly
     * from any other layer.
     */
    @Query("SELECT * FROM contacts WHERE phone = :phone LIMIT 1")
    suspend fun findByPhone(phone: String): CallerInfo?

    /**
     * Inserts all seed records on first launch.
     * IGNORE strategy prevents duplicate inserts on subsequent launches.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(contacts: List<CallerInfo>)

    /** Row count — used to check if seeding is needed. */
    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun count(): Int

    /** Returns all contacts — used for debugging. */
    @Query("SELECT * FROM contacts")
    suspend fun getAll(): List<CallerInfo>
}
