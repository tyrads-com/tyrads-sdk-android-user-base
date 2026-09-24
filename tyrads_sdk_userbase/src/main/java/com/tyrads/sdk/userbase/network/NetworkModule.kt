package com.tyrads.sdk.userbase.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.tyrads.sdk.userbase.config.TyradsConfig
import com.tyrads.sdk.userbase.session.SessionStore
import com.tyrads.sdk.userbase.util.Logger
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Builds the shared Retrofit/OkHttp client once, off whatever [TyradsConfig.baseUrl] resolves to
 * at construction time — matching both the RN and native SDKs' "environment must be set before
 * init" contract (there's no live re-pointing after this is built).
 */
@OptIn(ExperimentalSerializationApi::class)
internal class NetworkModule(sessionStore: SessionStore) {

    val json = TyradsJson

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor(sessionStore))
        .apply {
            if (Logger.debugMode) {
                addInterceptor(
                    HttpLoggingInterceptor { message -> Logger.d(message) }
                        .setLevel(HttpLoggingInterceptor.Level.BODY),
                )
            }
        }
        .build()

    val api: TyradsApiService = Retrofit.Builder()
        .baseUrl(TyradsConfig.baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(TyradsApiService::class.java)

    fun jsonBody(text: String) = text.toRequestBody("application/json".toMediaType())
}
