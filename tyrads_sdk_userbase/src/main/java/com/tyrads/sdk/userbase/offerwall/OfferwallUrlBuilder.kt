package com.tyrads.sdk.userbase.offerwall

import android.net.Uri
import com.tyrads.sdk.userbase.config.TyradsConfig
import com.tyrads.sdk.userbase.models.TyradsOfferwallUrlOptions
import com.tyrads.sdk.userbase.session.SessionStore

/**
 * Pure client-side URL builder — no WebView, no network call. The host app renders this URL
 * however it wants (this is the "headless" User Base contract, matching the RN SDK).
 */
internal object OfferwallUrlBuilder {
    fun build(sessionStore: SessionStore, options: TyradsOfferwallUrlOptions?): String {
        val token = sessionStore.token
        check(!token.isNullOrEmpty()) { "No active session — call loginUser() first" }

        val builder = Uri.parse(TyradsConfig.webSdkHost).buildUpon()
            .appendQueryParameter("token", token)
            .appendQueryParameter("lang", sessionStore.currentLanguage)

        options?.route?.let { builder.appendQueryParameter("to", it) }
        options?.campaignId?.let { builder.appendQueryParameter("campaignId", it) }
        options?.placementId?.let { builder.appendQueryParameter("placementId", it.toString()) }

        return builder.build().toString()
    }
}
