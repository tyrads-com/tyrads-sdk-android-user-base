package com.tyrads.sdk.userbase.models

import kotlinx.serialization.Serializable

@Serializable
internal data class TyradsInitRequest(
    val publisherUserId: String,
    val platform: String,
    val deviceData: TyradsDeviceData,
    val identifierType: String,
    val identifier: String,
    val devicePushToken: String? = null,
    val engagementId: Int? = null,
    val placementId: Int? = null,
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
    val email: String? = null,
    val phoneNumber: String? = null,
    val userGroup: String? = null,
    val age: Int? = null,
    val gender: Int? = null,
)

@Serializable
data class TyradsInitResponse(
    val code: Int,
    val message: String,
    val timestamp: Long,
    val responseTime: Double,
    val data: TyradsInitData,
)

@Serializable
data class TyradsInitData(
    val newRegisteredUser: Boolean = false,
    val newRegisteredDevice: Boolean = false,
    val accountInfo: TyradsAccountInfo,
    val appInfo: TyradsAppInfo,
    val token: String,
)

@Serializable
data class TyradsAccountInfo(
    val id: Long,
    val publisherUserId: String,
)

@Serializable
data class TyradsAppInfo(
    val headerColor: String = "",
    val mainColor: String = "",
    val premiumColor: String = "",
)

/** Local, in-memory snapshot returned by `getSession()`, not a network response. */
data class TyradsSession(
    val userId: String,
    val token: String?,
    val isLoginSuccessful: Boolean,
    val currentLanguage: String,
)

@Serializable
internal data class TyradsEncryptedEnvelope(
    val `val`: String,
    val vec: String,
    val tag: String,
)
