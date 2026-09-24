package com.tyrads.sdk.userbase.constants

/**
 * Route constants accepted by [com.tyrads.sdk.userbase.models.TyradsOfferwallUrlOptions.route] —
 * the same routes the native SDKs use for `showOffers(route:)`. `route` is a plain string param,
 * so any other value the backend recognizes works too; these are just the named ones.
 */
object TyradsDeepRoutes {
    const val OFFERS = "offers"
    const val ACTIVE_OFFERS = "activeOffers"
    const val SUPPORT = "support"
    const val SETTINGS = "settings"
}
