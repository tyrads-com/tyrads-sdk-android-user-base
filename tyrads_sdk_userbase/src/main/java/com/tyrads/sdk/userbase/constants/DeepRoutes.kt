package com.tyrads.sdk.userbase.constants

/**
 * Route constants accepted by [com.tyrads.sdk.userbase.models.TyradsOfferwallUrlOptions.route], for
 * jumping straight into a sub-page of the web offerwall. Omit `route` for the main offerwall. There's
 * no bundled UI here, so `offers`/`activeOffers` aren't meaningful deep-link targets the way they are
 * for the native SDKs' own webview (mirrors the RN User Base SDK, which dropped them too). `route` is
 * a plain string param, so any other value the backend recognizes still works.
 */
object TyradsDeepRoutes {
    const val SUPPORT = "support"
    const val SETTINGS = "settings"
}
