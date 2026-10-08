package com.tyrads.sdk.userbase

import android.content.Context
import androidx.annotation.Keep
import com.tyrads.sdk.userbase.callbacks.TyradsCallback
import com.tyrads.sdk.userbase.callbacks.TyradsResultCallback
import com.tyrads.sdk.userbase.config.TyradsConfig
import com.tyrads.sdk.userbase.constants.TyradsActivity
import com.tyrads.sdk.userbase.device.DeviceDataCollector
import com.tyrads.sdk.userbase.models.TyradsInitOptions
import com.tyrads.sdk.userbase.models.TyradsOfferwallUrlOptions
import com.tyrads.sdk.userbase.models.TyradsSession
import com.tyrads.sdk.userbase.network.NetworkModule
import com.tyrads.sdk.userbase.network.TyradsJson
import com.tyrads.sdk.userbase.network.TyradsRepository
import com.tyrads.sdk.userbase.offerwall.OfferwallUrlBuilder
import com.tyrads.sdk.userbase.push.TyradsPushToken
import com.tyrads.sdk.userbase.session.SessionStore
import com.tyrads.sdk.userbase.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor

/**
 * Headless, API-only TyrAds SDK with no bundled offerwall UI. Network calls return the raw API
 * response body as a JSON `String`, untouched, so the host app parses it however it likes. Every
 * method mirrors the RN
 * `@tyrads.com/tyrads-sdk-react-native-user-base` package's public surface 1:1, backed by the same
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
        interceptors: List<Interceptor> = emptyList(),
    ) {
        Logger.debugMode = debugMode
        appContext = context.applicationContext
        val store = SessionStore(appContext)
        store.restore()
        store.setCredentials(apiKey, apiSecret, encKey)
        sessionStore = store
        val networkModule = NetworkModule(store, interceptors)
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
        interceptors: List<Interceptor> = emptyList(),
        callback: TyradsCallback,
    ) {
        callbackScope.launch {
            runCatching { init(context, apiKey, apiSecret, encKey, debugMode, interceptors) }
                .onSuccess { callback.onSuccess() }
                .onFailure { callback.onFailure(it.message ?: "Unknown error") }
        }
    }

    // endregion

    // region session

    /** `POST initialize`. Returns the raw response body (JSON). */
    suspend fun loginUser(userId: String, options: TyradsInitOptions? = null): String {
        ensureInitialized()
        val deviceData = DeviceDataCollector.collect(appContext)
        val identity = DeviceDataCollector.resolveAdvertisingIdentity(appContext, sessionStore)
        val pushToken = TyradsPushToken.get(appContext)
        val response = repository.login(
            userId, deviceData, identity.identifierType, identity.identifier, pushToken, options,
        )
        val data = TyradsJson.parseToJsonElement(response).jsonObject.getValue("data").jsonObject
        sessionStore.setUser(
            data.getValue("accountInfo").jsonObject.getValue("publisherUserId").jsonPrimitive.content,
            data.getValue("token").jsonPrimitive.content,
        )
        runCatching { repository.trackActivity(TyradsActivity.INITIALIZED) }
            .onFailure { Logger.w("Failed to track Initialized activity", it) }
        return response
    }

    @JvmOverloads
    fun loginUser(userId: String, options: TyradsInitOptions? = null, callback: TyradsResultCallback<String>) {
        callbackScope.launch {
            runCatching { loginUser(userId, options) }
                .onSuccess { callback.onSuccess(it) }
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

    /** `GET campaigns?mode=userbase`. Returns the raw response body (JSON). */
    suspend fun getCampaigns(): String {
        ensureInitialized()
        return repository.getCampaigns(sessionStore.currentLanguage)
    }

    fun getCampaigns(callback: TyradsResultCallback<String>) {
        callbackScope.launch {
            runCatching { getCampaigns() }
                .onSuccess { callback.onSuccess(it) }
                .onFailure { callback.onFailure(it.message ?: "Unknown error") }
        }
    }

    /** `GET campaigns/:id?mode=userbase`. Returns the raw response body (JSON). */
    suspend fun getCampaignDetail(campaignId: String): String {
        ensureInitialized()
        return repository.getCampaignDetail(sessionStore.currentLanguage, campaignId)
    }

    fun getCampaignDetail(campaignId: String, callback: TyradsResultCallback<String>) {
        callbackScope.launch {
            runCatching { getCampaignDetail(campaignId) }
                .onSuccess { callback.onSuccess(it) }
                .onFailure { callback.onFailure(it.message ?: "Unknown error") }
        }
    }

    /** `GET campaigns/activated`. Returns the raw response body (JSON). */
    suspend fun getActivatedCampaigns(): String {
        ensureInitialized()
        return repository.getActivatedCampaigns(sessionStore.currentLanguage)
    }

    fun getActivatedCampaigns(callback: TyradsResultCallback<String>) {
        callbackScope.launch {
            runCatching { getActivatedCampaigns() }
                .onSuccess { callback.onSuccess(it) }
                .onFailure { callback.onFailure(it.message ?: "Unknown error") }
        }
    }

    /** `GET campaigns/activated/summary`, unwrapped to `data.activeCampaignCount`. */
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

    /** `GET account/engagement`, unwrapped to `data.CurrencySales` (JSON), or null when no sale is running. */
    suspend fun getEngagement(): String? {
        ensureInitialized()
        return repository.getEngagement(sessionStore.currentLanguage)
    }

    fun getEngagement(callback: TyradsResultCallback<String?>) {
        callbackScope.launch {
            runCatching { getEngagement() }
                .onSuccess { callback.onSuccess(it) }
                .onFailure { callback.onFailure(it.message ?: "Unknown error") }
        }
    }

    /** `POST campaigns/:id/activate`. Returns the raw response body (JSON). */
    suspend fun activateCampaign(campaignId: String): String {
        ensureInitialized()
        return repository.activateCampaign(campaignId)
    }

    fun activateCampaign(campaignId: String, callback: TyradsResultCallback<String>) {
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
