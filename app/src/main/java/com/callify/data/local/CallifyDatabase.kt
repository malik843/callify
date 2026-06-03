package com.callify.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.callify.data.model.CallerInfo

@Database(entities = [CallerInfo::class], version = 1, exportSchema = false)
abstract class CallifyDatabase : RoomDatabase() {

    abstract fun callerDao(): CallerDao

    companion object {
        @Volatile private var INSTANCE: CallifyDatabase? = null

        fun getInstance(context: Context): CallifyDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    CallifyDatabase::class.java,
                    "callify_contacts.db"
                )
                .fallbackToDestructiveMigration(true)
                .build().also { INSTANCE = it }
            }
        }
    }
}
