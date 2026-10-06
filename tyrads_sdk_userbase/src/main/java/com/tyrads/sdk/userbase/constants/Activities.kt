package com.tyrads.sdk.userbase.constants

/**
 * Exact PascalCase activity names expected by the backend, verified against
 * TrackingActivities.swift (native iOS SDK) and TyradsActivity (native Android SDK).
 * A prior camelCase mismatch caused live 422s, so these values must not be reformatted.
 */
object TyradsActivity {
    const val INITIALIZED = "Initialized"
    const val PROFILE_UPDATED = "ProfileUpdated"
    const val OPENED = "Opened"
    const val TARGETED_CAMPAIGN_SHOWN = "TargetedCampaignShown"
    const val TARGETED_CAMPAIGN_DETAIL_SHOWN = "TargetedCampaignDetailShown"
    const val CAMPAIGN_ACTIVATED = "CampaignActivated"
    const val CAMPAIGN_ACTIVATED_RETRY = "CampaignActivatedRetry"
    const val SUPPORT_TICKET_SHOWN = "SupportTicketShown"
    const val CLOSED = "Closed"
}
