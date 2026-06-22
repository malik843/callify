package com.callify.data.local

import android.content.Context
import android.util.Log
import com.callify.BuildConfig
import com.callify.data.model.CallerResult
import com.callify.data.model.CallLogEntry
import com.callify.utils.ReceiverNumberResolver
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Silent call log writer.
 *
 * Writes one record to the call_log table after every incoming
 * call lookup. Never surfaces data to the UI layer.
 *
 * ─────────────────────────────────────────────────────────────
 * ACCESS: ADB shell only (debug builds)
 *   adb shell run-as com.callify.debug sqlite3 \
 *     /data/data/com.callify.debug/databases/callify_contacts.db
 * ─────────────────────────────────────────────────────────────
 */
class CallLogWriter(
    private val dao: CallLogDao,
    private val context: Context
) {

    /**
     * Builds and inserts a [CallLogEntry] from the lookup result.
     * Must be called from a coroutine on Dispatchers.IO.
     *
     * @param incomingNumber  The normalised incoming number
     * @param result          The [CallerResult] from [com.callify.repository.CallerRepository]
     */
    suspend fun write(incomingNumber: String, result: CallerResult) {
        val now      = LocalDateTime.now()
        val date     = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val time     = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val receiver = ReceiverNumberResolver.resolve(context)

        val caller = when (result) {
            is CallerResult.Found -> {
                val info = result.info
                "${info.firstname.orEmpty()} ${info.lastname.orEmpty()}".trim()
                    .ifEmpty { incomingNumber }
            }
            is CallerResult.NotFound     -> incomingNumber
            is CallerResult.Timeout      -> incomingNumber
            is CallerResult.NetworkError -> incomingNumber
        }

        val entry = CallLogEntry(
            date           = date,
            time           = time,
            caller         = caller,
            receiverNumber = receiver
        )

        try {
            dao.insert(entry)
            if (BuildConfig.DEBUG) {
                Log.d("Callify", "CallLog written — $date $time | $caller → $receiver")
            }
        } catch (e: Exception) {
            Log.e("Callify", "CallLog write failed: ${e.message}")
        }
    }
}
