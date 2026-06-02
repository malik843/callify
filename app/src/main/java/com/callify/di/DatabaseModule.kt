package com.callify.di

import android.content.Context
import com.callify.data.local.CallerDao
import com.callify.data.local.CallifyDatabase
import com.callify.data.local.MockCallerDataSource
import com.callify.data.model.CallerInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Collections
import java.util.LinkedHashMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CallifyDatabase {
        return CallifyDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideCallerDao(database: CallifyDatabase): CallerDao {
        return database.callerDao()
    }

    @Provides
    @Singleton
    fun provideMockCallerDataSource(dao: CallerDao): MockCallerDataSource {
        return MockCallerDataSource(dao)
    }

    @Provides
    @Singleton
    fun provideCallerCache(): MutableMap<String, CallerInfo> {
        return Collections.synchronizedMap(
            object : LinkedHashMap<String, CallerInfo>(50, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CallerInfo>): Boolean {
                    return size > 50
                }
            }
        )
    }
}
