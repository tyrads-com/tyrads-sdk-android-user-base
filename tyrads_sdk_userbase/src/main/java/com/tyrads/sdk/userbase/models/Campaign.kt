package com.tyrads.sdk.userbase.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class TyradsMeta(
    val itemCount: Int = 0,
)

@Serializable
data class TyradsOffersResponse<T>(
    val code: Int,
    val data: List<T>,
    val meta: TyradsMeta? = null,
    val message: String,
    val timestamp: Long,
    val responseTime: Double,
)

@Serializable
internal data class TyradsSingleResponse<T>(
    val code: Int,
    val data: T,
    val message: String,
    val timestamp: Long,
    val responseTime: Double,
)

@Serializable
data class AvailableCurrency(
    val currencyId: Int,
    // Nullable on the wire; defaults (with coerceInputValues) keep a null from crashing the parse.
    val currencyIcon: String = "",
    val currencyName: String = "",
)

@Serializable
data class PayoutSummary(
    val totalPayoutConverted: Double = 0.0,
    val totalPlayablePayoutConverted: Double = 0.0,
    val totalMicrochargePayoutConverted: Double = 0.0,
)

/** Speed bonus for completing an event faster. Shape per the User Base API Reference. */
@Serializable
data class ShorterMaxTimeRule(
    /** Extra reward per currency ID, e.g. `{ "1": { "shorterMaxTimeAdditionConverted": 25 } }`. */
    val shorterMaxTimePayout: Map<String, ShorterMaxTimePayout> = emptyMap(),
    val shorterMaxTimeRemainSeconds: Long? = null,
    /** `ShorterMaxTime` or `Lto` (limited-time offer). */
    val specialCompletionReason: String? = null,
)

@Serializable
data class ShorterMaxTimePayout(
    val shorterMaxTimeAdditionConverted: Double = 0.0,
)

/** Level-based reward multiplier. `null` on the campaign when the feature isn't active. */
@Serializable
data class CampaignStage(
    val level: Int? = null,
    val multiplier: Double? = null,
    val nextLevel: Int? = null,
    val nextMultiplier: Double? = null,
)

@Serializable
data class CampaignTracking(
    val impressionUrl: String? = null,
    val clickUrl: String? = null,
    val s2sClickUrl: String? = null,
)

@Serializable
data class CampaignEventPayout(
    val payoutAmountConverted: Double = 0.0,
)

@Serializable
data class CampaignEvent(
    val appEventId: Int,
    val identifier: String = "",
    // Shown as `#` while the event is hidden; nullable on the wire.
    val eventName: String = "",
    val eventDescription: String? = null,
    val allowDuplicateEvents: Boolean = false,
    val payoutInfo: Map<String, CampaignEventPayout> = emptyMap(),
    val rewardedOn: String? = null,
    val conversionStatus: String? = null,
    val lockEventRule: List<JsonElement> = emptyList(),
    val hideEventRule: List<JsonElement> = emptyList(),
    val shorterMaxTimeRule: ShorterMaxTimeRule? = null,
    val isTicketSubmitted: Boolean? = null,
    val ticketStatus: String? = null,
    val ticketUrl: String? = null,
    val ticketRejectReason: String? = null,
    val ticketRejectionCode: String? = null,
    val count: Int? = null,
    val limit: Int? = null,
    val maxTime: Int = 0,
    val maxTimeMetric: String? = null,
    val maxTimeRemainSeconds: Long? = null,
    val enforceMaxTimeCompletion: Boolean = false,
    val rewardingExpiredOn: String? = null,
    val rewardingExpiredInSeconds: Long? = null,
    /** `Playable`, `LimitedTime`, `ShorterMaxTime` or `Microcharge`. */
    val type: String = "",
    // LimitedTime events
    val isLimitedTimeEvent: Boolean = false,
    val limitedTimeEventRemainingSeconds: Long? = null,
    // Microcharge events
    val dailyCount: Int? = null,
    val dailyLimit: Int? = null,
    val totalDailyUniqueCount: Int? = null,
    val totalDailyUniqueLimit: Int? = null,
    val dailyUniqueTodayExist: Boolean? = null,
)

/**
 * Both `GET campaigns` (list) and `GET campaigns/:id` (detail) return this exact shape under
 * `mode=userbase`, confirmed against a live production account (2026-09-23); there is no
 * separate, richer "detail" schema in this mode unlike the full tyrads-sdk-android's API.
 */
@Serializable
data class Campaign(
    val campaignId: Int,
    val tracking: CampaignTracking = CampaignTracking(),
    // Nullable on the wire; defaults (with coerceInputValues) keep a null from crashing the parse.
    val packageName: String = "",
    val os: String = "",
    val title: String = "",
    val thumbnail: String? = null,
    val creativeUrl: String? = null,
    val campaignDescription: String? = null,
    val installedOn: String? = null,
    val activatedOn: String? = null,
    val uninstalledOn: String? = null,
    val expiredOn: String? = null,
    val expiredInSeconds: Long? = null,
    val currencies: List<AvailableCurrency> = emptyList(),
    val activeCurrencyId: Int? = null,
    val payoutSummary: Map<String, PayoutSummary> = emptyMap(),
    val earnedPayout: Map<String, JsonElement> = emptyMap(),
    val stage: CampaignStage? = null,
    val engagements: List<JsonElement> = emptyList(),
    val events: List<CampaignEvent> = emptyList(),
)

@Serializable
data class CurrencySales(
    val name: String? = null,
    val multiplier: Double? = null,
    val bannerUrl: String? = null,
    val dateStart: String? = null,
    val dateEnd: String? = null,
    val remainingTimeSeconds: Long? = null,
)

@Serializable
data class ActivatedCampaignsResponse(
    val data: List<ActivatedCampaignsGroup> = emptyList(),
    val message: String = "",
)

@Serializable
data class ActivatedCampaignsGroup(
    val groupName: String = "",
    val availableCurrencies: Map<String, AvailableCurrency> = emptyMap(),
    val campaigns: List<ActivatedCampaign> = emptyList(),
)

@Serializable
data class ActivatedCampaignValidity(
    val isRetryDownload: Boolean = false,
    val isActivated: Boolean = false,
    val isOldUser: Boolean = false,
    val expiredOn: String? = null,
    val expiredInSeconds: Long? = null,
    val isInstalled: Boolean = false,
    val activeCurrencyId: Int? = null,
    val capReached: Boolean = false,
)

@Serializable
data class PayoutInfo(
    val currencyId: Int? = null,
    val currencyName: String? = null,
    val currencyIcon: String? = null,
    val currencyConversionRate: Double = 0.0,
    val payoutAmountConverted: Double = 0.0,
)

@Serializable
data class LimitedTimeEvent(
    val appEventId: Int,
    val conversionStatus: String? = null,
    val identifier: String = "",
    val eventName: String = "",
    val eventDescription: String? = null,
    val eventCategory: String? = null,
    val payoutInfo: Map<String, PayoutInfo> = emptyMap(),
    val allowDuplicateEvents: Boolean = false,
    val maxTime: Long = 0,
    val maxTimeMetric: String? = null,
    val maxTimeRemainSeconds: Long? = null,
    val enforceMaxTimeCompletion: Boolean = false,
    val isLimitedTimeEvent: Boolean = false,
    val limitedTimeEventRemainingSeconds: Long = 0,
    val isTicketSubmitted: Boolean = false,
    val ticketStatus: String? = null,
    val lockEventRule: JsonElement? = null,
    val hideEventRule: JsonElement? = null,
    val shorterMaxTimeRule: ShorterMaxTimeRule? = null,
    val specialCompletionReason: String? = null,
    val dailyCount: Int = 0,
    val dailyLimit: Int? = null,
    val count: Int = 0,
    val limit: Int? = null,
    val totalDailyUniqueCount: Int = 0,
    val totalDailyUniqueLimit: Int? = null,
    val dailyUniqueTodayExist: Boolean = false,
)

@Serializable
data class CampaignEventSummary(
    val playableEventCountAvailable: Int = 0,
    val playableEventCountCompleted: Int = 0,
    val playableEventCountTotal: Int = 0,
    val microchargeEventCountAvailable: Int = 0,
    val microchargeEventCountCompleted: Int = 0,
    val microchargeEventCountTotal: Int = 0,
)

/**
 * Ported from the RN SDK's TypeScript types. Confirmed against a live production account with an
 * activated campaign (2026-10-06).
 */
@Serializable
data class ActivatedCampaign(
    val campaignId: Int,
    val campaignName: String? = null,
    val campaignDescription: String? = null,
    val campaignType: String? = null,
    val campaignPremium: Boolean = false,
    val validity: ActivatedCampaignValidity = ActivatedCampaignValidity(),
    val availableCurrencies: Map<String, AvailableCurrency> = emptyMap(),
    val campaignStatus: String? = null,
    val group: String? = null,
    // Shape not confirmed for activated campaigns yet, so kept raw.
    val stage: JsonElement? = null,
    val eventSummary: CampaignEventSummary = CampaignEventSummary(),
    val limitedTimeEvents: List<LimitedTimeEvent> = emptyList(),
    val shorterMaxTimeEvents: List<JsonElement> = emptyList(),
)
