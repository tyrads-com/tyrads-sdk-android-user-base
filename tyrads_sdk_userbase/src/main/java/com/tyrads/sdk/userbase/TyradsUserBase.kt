package com.tyrads.sdk.userbase

import android.content.Context
import androidx.annotation.Keep
import com.tyrads.sdk.userbase.callbacks.TyradsCallback
import com.tyrads.sdk.userbase.callbacks.TyradsLoginCallback
import com.tyrads.sdk.userbase.callbacks.TyradsResultCallback
import com.tyrads.sdk.userbase.config.TyradsConfig
import com.tyrads.sdk.userbase.constants.TyradsActivity
import com.tyrads.sdk.userbase.device.DeviceDataCollector
import com.tyrads.sdk.userbase.models.ActivatedCampaignsResponse
import com.tyrads.sdk.userbase.models.Campaign
import com.tyrads.sdk.userbase.models.CurrencySales
import com.tyrads.sdk.userbase.models.TyradsInitOptions
import com.tyrads.sdk.userbase.models.TyradsInitResponse
import com.tyrads.sdk.userbase.models.TyradsOfferwallUrlOptions
import com.tyrads.sdk.userbase.models.TyradsSession
import com.tyrads.sdk.userbase.network.NetworkModule
import com.tyrads.sdk.userbase.network.TyradsRepository
import com.tyrads.sdk.userbase.offerwall.OfferwallUrlBuilder
import com.tyrads.sdk.userbase.session.SessionStore
import com.tyrads.sdk.userbase.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

/**
 * Headless, API-only TyrAds SDK — no bundled offerwall UI. Every method mirrors the RN
 * `@tyrads.com/tyrads-sdk-react-user-base` package's public surface 1:1, backed by the same
 * device-data / activity-tracking contract as the native tyrads-sdk-android.
 *
 * Call [init] once, then [loginUser] before any campaign/tracking/offerwall call.
 */
@Keep
object TyradsUserBase {

    private lateinit var sessionStore: SessionStore
    private lateinit var repository: TyradsRepository
    private lateinit var appContext: Context

    private val callbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private fun ensureInitialized() {
        check(this::repository.isInitialized) { "TyradsUserBase.init() must be called before any other method" }
    }

    // region init

    suspend fun init(
        context: Context,
        apiKey: String,
        apiSecret: String,
        encKey: String? = null,
        debugMode: Boolean = false,
    ) {
        Logger.debugMode = debugMode
        appContext = context.applicationContext
        val store = SessionStore(appContext)
        store.restore()
        store.setCredentials(apiKey, apiSecret, encKey)
        sessionStore = store
        val networkModule = NetworkModule(store)
        repository = TyradsRepository(networkModule, store)
        Logger.d("Initialized (env=${TyradsConfig.environment})")
    }

    @JvmOverloads
    fun init(
        context: Context,
        apiKey: String,
        apiSecret: String,
        encKey: String? = null,
        debugMode: Boolean = false,
        callback: TyradsCallback,
    ) {
        callbackScope.launch {
            runCatching { init(context, apiKey, apiSecret, encKey, debugMode) }
                .onSuccess { callback.onSuccess() }
                .onFailure { callback.onFailure(it.message ?: "Unknown error") }
        }
    }

    // endregion

    // region session

    suspend fun loginUser(userId: String, options: TyradsInitOptions? = null): TyradsInitResponse {
        ensureInitialized()
        val deviceData = DeviceDataCollector.collect(appContext)
        val identity = DeviceDataCollector.resolveAdvertisingIdentity(appContext, sessionStore)
        val response = repository.login(userId, deviceData, identity.identifierType, identity.identifier, options)
        sessionStore.setUser(response.data.accountInfo.publisherUserId, response.data.token)
        runCatching { repository.trackActivity(TyradsActivity.INITIALIZED) }
            .onFailure { Logger.w("Failed to track Initialized activity", it) }
        return response
    }

    @JvmOverloads
    fun loginUser(userId: String, options: TyradsInitOptions? = null, callback: TyradsLoginCallback) {
        callbackScope.launch {
            runCatching { loginUser(userId, options) }
                .onSuccess { callback.onSuccess(it.data.newRegisteredUser) }
                .onFailure { callback.onFailure(it.message ?: "Unknown error") }
        }
    }

    fun logoutUser() {
        ensureInitialized()
        sessionStore.clearSession()
    }

    fun logoutUser(callback: TyradsCallback) {
        logoutUser()
        callback.onSuccess()
    }

    fun changeLanguage(lang: String) {
        ensureInitialized()
        sessionStore.setLanguage(lang)
    }

    fun getSession(): TyradsSession {
        ensureInitialized()
        return TyradsSession(
            userId = sessionStore.userId,
            token = sessionStore.token,
            isLoginSuccessful = sessionStore.isLoggedIn,
            currentLanguage = sessionStore.currentLanguage,
        )
    }

    // endregion

    // region campaigns

    suspend fun getCampaigns(): List<Campaign> {
        ensureInitialized()
        return repository.getCampaigns(sessionStore.currentLanguage)
    }

    fun getCampaigns(callback: TyradsResultCallback<List<Campaign>>) {
        callbackScope.launch {
            runCatching { getCampaigns() }
                .onSuccess { callback.onSuccess(it) }
                .onFailure { callback.onFailure(it.message ?: "Unknown error") }
        }
    }

    suspend fun getCampaignDetail(campaignId: String): Campaign {
        ensureInitialized()
        return repository.getCampaignDetail(sessionStore.currentLanguage, campaignId)
    }

    fun getCampaignDetail(campaignId: String, callback: TyradsResultCallback<Campaign>) {
        callbackScope.launch {
            runCatching { getCampaignDetail(campaignId) }
                .onSuccess { callback.onSuccess(it) }
                .onFailure { callback.onFailure(it.message ?: "Unknown error") }
        }
    }

    suspend fun getActivatedCampaigns(): ActivatedCampaignsResponse {
        ensureInitialized()
        return repository.getActivatedCampaigns(sessionStore.currentLanguage)
    }

    fun getActivatedCampaigns(callback: TyradsResultCallback<ActivatedCampaignsResponse>) {
        callbackScope.launch {
            runCatching { getActivatedCampaigns() }
                .onSuccess { callback.onSuccess(it) }
                .onFailure { callback.onFailure(it.message ?: "Unknown error") }
        }
    }

    suspend fun getActivatedSummary(): Int {
        ensureInitialized()
        return repository.getActivatedSummary(sessionStore.currentLanguage)
    }

    fun getActivatedSummary(callback: TyradsResultCallback<Int>) {
        callbackScope.launch {
            runCatching { getActivatedSummary() }
                .onSuccess { callback.onSuccess(it) }
                .onFailure { callback.onFailure(it.message ?: "Unknown error") }
        }
    }

    suspend fun getEngagement(): CurrencySales? {
        ensureInitialized()
        return repository.getEngagement(sessionStore.currentLanguage)
    }

    fun getEngagement(callback: TyradsResultCallback<CurrencySales?>) {
        callbackScope.launch {
            runCatching { getEngagement() }
                .onSuccess { callback.onSuccess(it) }
                .onFailure { callback.onFailure(it.message ?: "Unknown error") }
        }
    }

    suspend fun activateCampaign(campaignId: String): JsonElement {
        ensureInitialized()
        return repository.activateCampaign(campaignId)
    }

    fun activateCampaign(campaignId: String, callback: TyradsResultCallback<JsonElement>) {
        callbackScope.launch {
            runCatching { activateCampaign(campaignId) }
                .onSuccess { callback.onSuccess(it) }
                .onFailure { callback.onFailure(it.message ?: "Unknown error") }
        }
    }

    // endregion

    // region tracking

    suspend fun trackActivity(activity: String) {
        ensureInitialized()
        repository.trackActivity(activity)
    }

    fun trackActivity(activity: String, callback: TyradsCallback) {
        callbackScope.launch {
            runCatching { trackActivity(activity) }
                .onSuccess { callback.onSuccess() }
                .onFailure { callback.onFailure(it.message ?: "Unknown error") }
        }
    }

    // endregion

    // region offerwall

    @JvmOverloads
    fun getOfferwallUrl(options: TyradsOfferwallUrlOptions? = null): String {
        ensureInitialized()
        return OfferwallUrlBuilder.build(sessionStore, options)
    }

    // endregion
}
