package com.tyrads.sdk.userbase.network

import com.tyrads.sdk.userbase.constants.TyradsEndpoints
import com.tyrads.sdk.userbase.models.ActivatedCampaignsResponse
import com.tyrads.sdk.userbase.models.ActivatedSummaryResponse
import com.tyrads.sdk.userbase.models.Campaign
import com.tyrads.sdk.userbase.models.EngagementResponse
import com.tyrads.sdk.userbase.models.TyradsInitResponse
import com.tyrads.sdk.userbase.models.TyradsOffersResponse
import com.tyrads.sdk.userbase.models.TyradsSingleResponse
import kotlinx.serialization.json.JsonElement
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

internal interface TyradsApiService {

    @POST(TyradsEndpoints.INITIALIZE)
    suspend fun initialize(@Body body: RequestBody): Response<TyradsInitResponse>

    @GET(TyradsEndpoints.CAMPAIGNS)
    suspend fun getCampaigns(
        @Query("lang") lang: String,
        @Query("mode") mode: String = "userbase",
    ): Response<TyradsOffersResponse<Campaign>>

    @GET
    suspend fun getCampaignDetail(
        @Url path: String,
        @Query("lang") lang: String,
        @Query("mode") mode: String = "userbase",
    ): Response<TyradsSingleResponse<Campaign>>

    @GET(TyradsEndpoints.CAMPAIGNS_ACTIVATED)
    suspend fun getActivatedCampaigns(@Query("lang") lang: String): Response<ActivatedCampaignsResponse>

    @GET(TyradsEndpoints.CAMPAIGNS_ACTIVATED_SUMMARY)
    suspend fun getActivatedSummary(@Query("lang") lang: String): Response<ActivatedSummaryResponse>

    @GET(TyradsEndpoints.ENGAGEMENT)
    suspend fun getEngagement(@Query("lang") lang: String): Response<EngagementResponse>

    @POST
    suspend fun activateCampaign(@Url path: String, @Body body: RequestBody): Response<JsonElement>

    @POST(TyradsEndpoints.ACTIVITY)
    suspend fun trackActivity(@Body body: RequestBody): Response<JsonElement>
}
