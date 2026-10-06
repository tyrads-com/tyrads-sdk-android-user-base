package com.tyrads.sdk.userbase.session

import android.content.Context
import android.content.SharedPreferences
import com.tyrads.sdk.userbase.constants.TyradsKeyNames

/**
 * Holds credentials + session state in memory, persisted to SharedPreferences so a login survives
 * process death. Unlike the RN SDK, [restore] IS invoked automatically by `TyradsUserBase.init`,
 * so a previous login is picked back up on app relaunch by default.
 */
internal class SessionStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(TyradsKeyNames.PREFS_NAME, Context.MODE_PRIVATE)

    var apiKey: String = ""
        private set
    var apiSecret: String = ""
        private set
    var encKey: String? = null
        private set
    var userId: String = ""
        private set
    var token: String? = null
        private set
    var currentLanguage: String = "en-US"
        private set
    var placementId: Int? = null

    val isSecure: Boolean
        get() = !encKey.isNullOrBlank()

    val isLoggedIn: Boolean
        get() = userId.isNotEmpty() && !token.isNullOrEmpty()

    fun setCredentials(apiKey: String, apiSecret: String, encKey: String?) {
        this.apiKey = apiKey
        this.apiSecret = apiSecret
        this.encKey = encKey
        prefs.edit()
            .putString(TyradsKeyNames.API_KEY, apiKey)
            .putString(TyradsKeyNames.API_SECRET, apiSecret)
            .putString(TyradsKeyNames.ENC_KEY, encKey)
            .apply()
    }

    fun setUser(userId: String, token: String?) {
        this.userId = userId
        this.token = token
        prefs.edit()
            .putString(TyradsKeyNames.USER_ID, userId)
            .putString(TyradsKeyNames.TOKEN, token)
            .apply()
    }

    fun setLanguage(lang: String) {
        currentLanguage = lang
        prefs.edit().putString(TyradsKeyNames.CURRENT_LANGUAGE, lang).apply()
    }

    fun getOrCreateCustomAdId(generator: () -> String): String {
        prefs.getString(TyradsKeyNames.CUSTOM_AD_ID, null)?.let { return it }
        val generated = generator()
        prefs.edit().putString(TyradsKeyNames.CUSTOM_AD_ID, generated).apply()
        return generated
    }

    /** Re-hydrates credentials + session from SharedPreferences. Called once from `init()`. */
    fun restore() {
        apiKey = prefs.getString(TyradsKeyNames.API_KEY, "") ?: ""
        apiSecret = prefs.getString(TyradsKeyNames.API_SECRET, "") ?: ""
        encKey = prefs.getString(TyradsKeyNames.ENC_KEY, null)
        userId = prefs.getString(TyradsKeyNames.USER_ID, "") ?: ""
        token = prefs.getString(TyradsKeyNames.TOKEN, null)
        currentLanguage = prefs.getString(TyradsKeyNames.CURRENT_LANGUAGE, "en-US") ?: "en-US"
    }

    /** Clears the active session only. Credentials from `init()` are kept, matching the RN SDK. */
    fun clearSession() {
        userId = ""
        token = null
        prefs.edit()
            .remove(TyradsKeyNames.USER_ID)
            .remove(TyradsKeyNames.TOKEN)
            .apply()
    }
}
