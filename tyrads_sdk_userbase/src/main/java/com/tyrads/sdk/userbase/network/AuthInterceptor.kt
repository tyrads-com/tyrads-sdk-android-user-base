package com.tyrads.sdk.userbase.network

import com.tyrads.sdk.userbase.config.TyradsConfig
import com.tyrads.sdk.userbase.constants.TyradsEndpoints
import com.tyrads.sdk.userbase.session.SessionStore
import okhttp3.Interceptor
import okhttp3.Response

internal class AuthInterceptor(private val sessionStore: SessionStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val isInitializeCall = original.url.encodedPath.endsWith(TyradsEndpoints.INITIALIZE)

        val builder = original.newBuilder()
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("X-API-Key", sessionStore.apiKey)
            .header("X-API-Secret", sessionStore.apiSecret)
            .header("X-SDK-Platform", TyradsConfig.SDK_PLATFORM)
            .header("X-SDK-Version", TyradsConfig.SDK_VERSION)
            .header("X-Secure-Mode", if (sessionStore.isSecure) "BASIC" else "PLAIN")

        if (!isInitializeCall) {
            builder.header("X-User-ID", sessionStore.userId)
        }

        return chain.proceed(builder.build())
    }
}
