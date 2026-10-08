package com.tyrads.sdk.userbase.network

import com.tyrads.sdk.userbase.constants.TyradsEndpoints
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Every call returns the raw response body. The SDK hands that JSON to the host app untouched
 * (same as the RN User Base SDK), so no response models live here.
 */
internal interface TyradsApiService {

    @POST(TyradsEndpoints.INITIALIZE)
    suspend fun initialize(@Body body: RequestBody): Response<ResponseBody>

    @GET(TyradsEndpoints.CAMPAIGNS)
    suspend fun getCampaigns(
        @Query("lang") lang: String,
        @Query("mode") mode: String = "userbase",
    ): Response<ResponseBody>

    @GET
    suspend fun getCampaignDetail(
        @Url path: String,
        @Query("lang") lang: String,
        @Query("mode") mode: String = "userbase",
    ): Response<ResponseBody>

    @GET(TyradsEndpoints.CAMPAIGNS_ACTIVATED)
    suspend fun getActivatedCampaigns(@Query("lang") lang: String): Response<ResponseBody>

    @GET(TyradsEndpoints.CAMPAIGNS_ACTIVATED_SUMMARY)
    suspend fun getActivatedSummary(@Query("lang") lang: String): Response<ResponseBody>

    @GET(TyradsEndpoints.ENGAGEMENT)
    suspend fun getEngagement(@Query("lang") lang: String): Response<ResponseBody>

    @POST
    suspend fun activateCampaign(@Url path: String, @Body body: RequestBody): Response<ResponseBody>

    @POST(TyradsEndpoints.ACTIVITY)
    suspend fun trackActivity(@Body body: RequestBody): Response<ResponseBody>
}
