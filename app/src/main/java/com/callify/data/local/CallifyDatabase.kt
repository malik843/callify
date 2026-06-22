package com.callify.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.callify.data.model.CallerInfo
import com.callify.data.model.CallLogEntry

/**
 * Main Room database for Callify.
 *
 * Version history:
 *  1 → initial release (contacts table only)
 *  2 → adds call_log table for silent backend call logging
 */
@Database(
    entities = [CallerInfo::class, CallLogEntry::class],
    version = 2,
    exportSchema = false
)
abstract class CallifyDatabase : RoomDatabase() {

    abstract fun callerDao(): CallerDao
    abstract fun callLogDao(): CallLogDao

    companion object {

        /**
         * Migrates from version 1 to version 2.
         * Creates the call_log table without touching the existing contacts table.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS call_log (
                        id              INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date            TEXT NOT NULL,
                        time            TEXT NOT NULL,
                        caller          TEXT NOT NULL,
                        receiver_number TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile private var INSTANCE: CallifyDatabase? = null

        fun getInstance(context: Context): CallifyDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    CallifyDatabase::class.java,
                    "callify_contacts.db"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
