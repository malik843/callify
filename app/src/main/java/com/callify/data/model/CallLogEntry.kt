package com.callify.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single call log record written after every incoming call.
 *
 * This entity is NEVER exposed through any application UI.
 * Access is exclusively via ADB shell SQLite queries on
 * debug builds. See ADB_LOG_COMMANDS.md for query reference.
 *
 * Table: call_log
 */
@Entity(tableName = "call_log")
data class CallLogEntry(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** Date of the call — stored as "YYYY-MM-DD" for easy SQL filtering. */
    @ColumnInfo(name = "date")
    val date: String,

    /** Time of the call — stored as "HH:MM:SS" (24-hour). */
    @ColumnInfo(name = "time")
    val time: String,

    /**
     * Caller identity.
     * Found     → "Firstname Lastname" (normalised, trimmed)
     * NotFound  → normalised 10-digit number string
     * Timeout / NetworkError → normalised 10-digit number string
     */
    @ColumnInfo(name = "caller")
    val caller: String,

    /**
     * The device's own SIM number (the receiving end).
     * Normalised to 10-digit format.
     * "unknown_receiver" if carrier does not provision it or permission is missing.
     */
    @ColumnInfo(name = "receiver_number")
    val receiverNumber: String
)
