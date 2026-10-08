package com.tyrads.sdk.userbase.constants

internal object TyradsKeyNames {
    private const val PREFIX = "tyrads_ub_"

    const val PREFS_NAME = "tyrads_sdk_userbase_prefs"

    const val API_KEY = "${PREFIX}apiKey"
    const val API_SECRET = "${PREFIX}apiSecret"
    const val ENC_KEY = "${PREFIX}encKey"
    const val USER_ID = "${PREFIX}xUserId"
    const val TOKEN = "${PREFIX}token"
    const val CURRENT_LANGUAGE = "${PREFIX}currentLanguage"
    const val CUSTOM_AD_ID = "${PREFIX}customAdId"
}
