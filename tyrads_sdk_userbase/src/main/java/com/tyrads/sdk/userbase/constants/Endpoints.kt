package com.tyrads.sdk.userbase.constants

internal object TyradsEndpoints {
    const val INITIALIZE = "initialize"
    const val CAMPAIGNS = "campaigns"
    const val CAMPAIGNS_ACTIVATED = "campaigns/activated"
    const val CAMPAIGNS_ACTIVATED_SUMMARY = "campaigns/activated/summary"
    const val ENGAGEMENT = "account/engagement"
    const val ACTIVITY = "account/activity"

    fun campaignDetail(campaignId: String): String = "campaigns/$campaignId"
    fun activateCampaign(campaignId: String): String = "campaigns/$campaignId/activate"
}
