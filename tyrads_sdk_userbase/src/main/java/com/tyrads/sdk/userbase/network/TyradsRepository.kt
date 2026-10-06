package com.tyrads.sdk.userbase.network

import com.tyrads.sdk.userbase.constants.TyradsEndpoints
import com.tyrads.sdk.userbase.crypto.AesGcmCrypto
import com.tyrads.sdk.userbase.models.ActivatedCampaignsResponse
import com.tyrads.sdk.userbase.models.Campaign
import com.tyrads.sdk.userbase.models.CurrencySales
import com.tyrads.sdk.userbase.models.TrackActivityRequest
import com.tyrads.sdk.userbase.models.TyradsDeviceData
import com.tyrads.sdk.userbase.models.TyradsInitOptions
import com.tyrads.sdk.userbase.models.TyradsInitRequest
import com.tyrads.sdk.userbase.models.TyradsInitResponse
import com.tyrads.sdk.userbase.session.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

/** Thin wrapper over [TyradsApiService] handling body encryption, error normalization and response unwrapping. */
internal class TyradsRepository(
    private val networkModule: NetworkModule,
    private val sessionStore: SessionStore,
) {
    private val api get() = networkModule.api
    private val json get() = networkModule.json

    suspend fun login(
        userId: String,
        deviceData: TyradsDeviceData,
        identifierType: String,
        identifier: String,
        devicePushToken: String?,
        options: TyradsInitOptions?,
    ): TyradsInitResponse {
        val request = TyradsInitRequest(
            publisherUserId = userId,
            platform = "Android",
            deviceData = deviceData,
            identifierType = identifierType,
            identifier = identifier,
            devicePushToken = devicePushToken,
            engagementId = options?.engagementId,
            placementId = options?.placementId,
            mediaSourceName = options?.mediaSourceInfo?.mediaSourceName,
            mediaCampaignName = options?.mediaSourceInfo?.mediaCampaignName,
            mediaSourceId = options?.mediaSourceInfo?.mediaSourceId,
            mediaSubSourceId = options?.mediaSourceInfo?.mediaSubSourceId,
            incentivized = options?.mediaSourceInfo?.incentivized,
            mediaAdsetName = options?.mediaSourceInfo?.mediaAdsetName,
            mediaAdsetId = options?.mediaSourceInfo?.mediaAdsetId,
            mediaCreativeName = options?.mediaSourceInfo?.mediaCreativeName,
            mediaCreativeId = options?.mediaSourceInfo?.mediaCreativeId,
            sub1 = options?.mediaSourceInfo?.sub1,
            sub2 = options?.mediaSourceInfo?.sub2,
            sub3 = options?.mediaSourceInfo?.sub3,
            sub4 = options?.mediaSourceInfo?.sub4,
            sub5 = options?.mediaSourceInfo?.sub5,
            email = options?.userInfo?.email,
            phoneNumber = options?.userInfo?.phoneNumber,
            userGroup = options?.userInfo?.userGroup,
            age = options?.userInfo?.age,
            gender = options?.userInfo?.gender,
        )
        val body = buildBody(TyradsInitRequest.serializer(), request)
        return execute("initialize") { api.initialize(body) }
    }

    suspend fun getCampaigns(lang: String): List<Campaign> =
        execute("campaigns") { api.getCampaigns(lang) }.data

    suspend fun getCampaignDetail(lang: String, campaignId: String): Campaign =
        execute("campaign detail") {
            api.getCampaignDetail(TyradsEndpoints.campaignDetail(campaignId), lang)
        }.data

    suspend fun getActivatedCampaigns(lang: String): ActivatedCampaignsResponse =
        execute("activated campaigns") { api.getActivatedCampaigns(lang) }

    suspend fun getActivatedSummary(lang: String): Int =
        execute("activated summary") { api.getActivatedSummary(lang) }.data.activeCampaignCount

    suspend fun getEngagement(lang: String): CurrencySales? =
        execute("engagement") { api.getEngagement(lang) }.data.CurrencySales

    suspend fun activateCampaign(campaignId: String): JsonElement {
        val body = networkModule.jsonBody("{}")
        return execute("activate campaign") { api.activateCampaign(TyradsEndpoints.activateCampaign(campaignId), body) }
    }

    suspend fun trackActivity(activity: String) {
        val body = buildBody(TrackActivityRequest.serializer(), TrackActivityRequest(activity))
        execute("track activity") { api.trackActivity(body) }
    }

    private fun <T> buildBody(serializer: kotlinx.serialization.KSerializer<T>, payload: T): okhttp3.RequestBody {
        val plainJson = json.encodeToString(serializer, payload)
        if (!sessionStore.isSecure) return networkModule.jsonBody(plainJson)

        val envelope = AesGcmCrypto.encrypt(plainJson, sessionStore.encKey!!)
        val envelopeJson = json.encodeToString(
            com.tyrads.sdk.userbase.models.TyradsEncryptedEnvelope.serializer(),
            envelope,
        )
        return networkModule.jsonBody(envelopeJson)
    }

    private suspend fun <T> execute(label: String, call: suspend () -> Response<T>): T =
        withContext(Dispatchers.IO) {
            val response = try {
                call()
            } catch (e: SocketTimeoutException) {
                throw TyradsHttpError.Timeout(e)
            } catch (e: IOException) {
                throw TyradsHttpError.Network(e)
            } catch (t: Throwable) {
                throw TyradsHttpError.Unknown(t)
            }

            if (!response.isSuccessful) {
                throw TyradsHttpError.Server(response.code(), response.errorBody()?.string())
            }
            response.body() ?: throw TyradsHttpError.Unknown(IllegalStateException("Empty body for $label"))
        }
}
