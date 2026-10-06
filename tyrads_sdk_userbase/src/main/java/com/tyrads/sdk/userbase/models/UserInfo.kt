package com.tyrads.sdk.userbase.models

/** Media-source/attribution info folded flat into the `initialize` request body. */
data class TyradsMediaSourceInfo(
    val mediaSourceName: String? = null,
    val mediaCampaignName: String? = null,
    val mediaSourceId: String? = null,
    val mediaSubSourceId: String? = null,
    val incentivized: Boolean? = null,
    val mediaAdsetName: String? = null,
    val mediaAdsetId: String? = null,
    val mediaCreativeName: String? = null,
    val mediaCreativeId: String? = null,
    val sub1: String? = null,
    val sub2: String? = null,
    val sub3: String? = null,
    val sub4: String? = null,
    val sub5: String? = null,
)

/** User profile info folded into the `initialize` request body. `gender`: 1 = male, 2 = female. */
data class TyradsUserInfo(
    val email: String? = null,
    val phoneNumber: String? = null,
    val userGroup: String? = null,
    val age: Int? = null,
    val gender: Int? = null,
)

data class TyradsInitOptions(
    val engagementId: Int? = null,
    val placementId: Int? = null,
    val mediaSourceInfo: TyradsMediaSourceInfo? = null,
    val userInfo: TyradsUserInfo? = null,
    @Deprecated("Ignored. The SDK fetches its own FCM token, scoped to TyrAds' Firebase project.")
    val devicePushToken: String? = null,
)
