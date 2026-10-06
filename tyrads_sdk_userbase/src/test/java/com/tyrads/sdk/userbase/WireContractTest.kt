package com.tyrads.sdk.userbase

import com.tyrads.sdk.userbase.config.TyradsConfig
import com.tyrads.sdk.userbase.config.TyradsEnvironment
import com.tyrads.sdk.userbase.constants.TyradsActivity
import com.tyrads.sdk.userbase.constants.TyradsDeepRoutes
import com.tyrads.sdk.userbase.constants.TyradsEndpoints
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the exact strings that go over the wire. Activity names are case-sensitive on the backend:
 * a camelCase variant returned 422s in another SDK, so the PascalCase values are locked down here.
 */
class WireContractTest {

    @After
    fun resetEnvironment() {
        TyradsConfig.setEnvironment(TyradsEnvironment.PRODUCTION)
    }

    @Test
    fun `activity names are PascalCase`() {
        assertEquals("Initialized", TyradsActivity.INITIALIZED)
        assertEquals("ProfileUpdated", TyradsActivity.PROFILE_UPDATED)
        assertEquals("Opened", TyradsActivity.OPENED)
        assertEquals("TargetedCampaignShown", TyradsActivity.TARGETED_CAMPAIGN_SHOWN)
        assertEquals("TargetedCampaignDetailShown", TyradsActivity.TARGETED_CAMPAIGN_DETAIL_SHOWN)
        assertEquals("CampaignActivated", TyradsActivity.CAMPAIGN_ACTIVATED)
        assertEquals("CampaignActivatedRetry", TyradsActivity.CAMPAIGN_ACTIVATED_RETRY)
        assertEquals("SupportTicketShown", TyradsActivity.SUPPORT_TICKET_SHOWN)
        assertEquals("Closed", TyradsActivity.CLOSED)
    }

    @Test
    fun `deep routes match the offerwall webapp`() {
        assertEquals("support", TyradsDeepRoutes.SUPPORT)
        assertEquals("settings", TyradsDeepRoutes.SETTINGS)
    }

    @Test
    fun `endpoints are relative paths without a leading slash`() {
        // Retrofit resolves @Url values relative to the base URL only when they have no leading slash
        assertEquals("initialize", TyradsEndpoints.INITIALIZE)
        assertEquals("campaigns", TyradsEndpoints.CAMPAIGNS)
        assertEquals("campaigns/activated", TyradsEndpoints.CAMPAIGNS_ACTIVATED)
        assertEquals("campaigns/activated/summary", TyradsEndpoints.CAMPAIGNS_ACTIVATED_SUMMARY)
        assertEquals("account/engagement", TyradsEndpoints.ENGAGEMENT)
        assertEquals("account/activity", TyradsEndpoints.ACTIVITY)
        assertEquals("campaigns/4785", TyradsEndpoints.campaignDetail("4785"))
        assertEquals("campaigns/4785/activate", TyradsEndpoints.activateCampaign("4785"))
    }

    @Test
    fun `environment defaults to production`() {
        assertEquals(TyradsEnvironment.PRODUCTION, TyradsConfig.environment)
        assertEquals("https://api.tyrads.com/v4.0/", TyradsConfig.baseUrl)
        assertEquals("https://v4.sdk.tyrads.com", TyradsConfig.webSdkHost)
    }

    @Test
    fun `staging swaps both the api and webapp hosts`() {
        TyradsConfig.setEnvironment(TyradsEnvironment.STAGING)

        assertEquals("https://api.stage.tyrads.com/v4.0/", TyradsConfig.baseUrl)
        assertEquals("https://staging.tyr-sdk-webapp-monorepo-v4.pages.dev", TyradsConfig.webSdkHost)
    }

    @Test
    fun `base url keeps its trailing slash so relative endpoints resolve`() {
        listOf(TyradsEnvironment.PRODUCTION, TyradsEnvironment.STAGING).forEach { env ->
            TyradsConfig.setEnvironment(env)
            assertEquals(true, TyradsConfig.baseUrl.endsWith("/"))
        }
    }

    @Test
    fun `sdk version is major-minor-patch-build`() {
        assertEquals("1.0.0-0", TyradsConfig.SDK_VERSION)
        assertEquals("Android-userbase", TyradsConfig.SDK_PLATFORM)
        assertEquals("4.0", TyradsConfig.API_VERSION)
    }
}
