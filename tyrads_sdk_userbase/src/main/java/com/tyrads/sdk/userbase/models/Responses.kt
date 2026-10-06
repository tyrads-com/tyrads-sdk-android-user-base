package com.tyrads.sdk.userbase.models

import kotlinx.serialization.Serializable

@Serializable
internal data class ActivatedSummaryResponse(
    val data: ActivatedSummaryData,
)

@Serializable
internal data class ActivatedSummaryData(
    val activeCampaignCount: Int = 0,
)

@Serializable
internal data class EngagementResponse(
    val data: EngagementData,
)

/** `CurrencySales` is `null` whenever there's no active currency-sale engagement. */
@Serializable
internal data class EngagementData(
    val CurrencySales: CurrencySales? = null,
)

@Serializable
internal data class TrackActivityRequest(
    val activity: String,
)
