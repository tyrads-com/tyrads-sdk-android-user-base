package com.tyrads.sdk.userbase.config

enum class TyradsEnvironment {
    STAGING,
    PRODUCTION,
}

/**
 * Environment / version constants for the User Base SDK.
 *
 * The native tyrads-sdk-android has no environment toggle (single hardcoded prod URL). This
 * mirrors the RN User Base SDK instead, which exposes [setEnvironment]. Call it (if at all)
 * before [com.tyrads.sdk.userbase.TyradsUserBase.init], since the network layer is built once
 * off whatever [BASE_URL] currently resolves to.
 */
object TyradsConfig {
    const val API_VERSION = "4.0"
    const val SDK_MAJOR = "1"
    const val SDK_MINOR = "0"
    const val SDK_PATCH = "0"
    const val SDK_BUILD = "0"
    const val SDK_VERSION = "$SDK_MAJOR.$SDK_MINOR.$SDK_PATCH-$SDK_BUILD"
    // "-userbase" suffix so the backend can tell this SDK apart from the full tyrads-sdk-android
    // (mirrors the RN User Base SDK's "React Native-userbase"). Sent as X-SDK-Platform and as
    // deviceData.sdkPlatform.
    const val SDK_PLATFORM = "Android-userbase"

    @Volatile
    var environment: TyradsEnvironment = TyradsEnvironment.PRODUCTION
        private set

    val baseUrl: String
        get() = when (environment) {
            TyradsEnvironment.STAGING -> "https://api.stage.tyrads.com/v$API_VERSION/"
            TyradsEnvironment.PRODUCTION -> "https://api.tyrads.com/v$API_VERSION/"
        }

    val webSdkHost: String
        get() = when (environment) {
            TyradsEnvironment.STAGING -> "https://staging.tyr-sdk-webapp-monorepo-v4.pages.dev"
            TyradsEnvironment.PRODUCTION -> "https://v4.sdk.tyrads.com"
        }

    fun setEnvironment(env: TyradsEnvironment) {
        environment = env
    }
}
