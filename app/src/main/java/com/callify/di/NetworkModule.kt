package com.callify.di

import com.callify.BuildConfig
import com.callify.data.model.CallerInfo
import com.callify.data.remote.CallerApiClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Collections
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module for providing network-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Provides a configured [OkHttpClient] with aggressive timeouts and logging.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY 
                    else HttpLoggingInterceptor.Level.NONE
        }

        return OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .writeTimeout(1, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(5, 30, TimeUnit.SECONDS))
            .retryOnConnectionFailure(false)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    /**
     * Provides the [Retrofit] instance configured with the base URL and Gson converter.
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.CALLIFY_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Provides the [CallerApiClient] interface implementation.
     */
    @Provides
    @Singleton
    fun provideCallerApiClient(retrofit: Retrofit): CallerApiClient {
        return retrofit.create(CallerApiClient::class.java)
    }

    /**
     * Provides an in-memory LRU cache for [CallerInfo] objects.
     * Max 50 entries, evicted based on least recently used.
     */
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
