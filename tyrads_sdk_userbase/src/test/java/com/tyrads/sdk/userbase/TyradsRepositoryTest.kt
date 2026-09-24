package com.tyrads.sdk.userbase

import android.util.Base64
import com.tyrads.sdk.userbase.models.ActivatedSummaryData
import com.tyrads.sdk.userbase.models.ActivatedSummaryResponse
import com.tyrads.sdk.userbase.models.Campaign
import com.tyrads.sdk.userbase.models.CampaignTracking
import com.tyrads.sdk.userbase.models.CurrencySales
import com.tyrads.sdk.userbase.models.EngagementData
import com.tyrads.sdk.userbase.models.EngagementResponse
import com.tyrads.sdk.userbase.models.TyradsOffersResponse
import com.tyrads.sdk.userbase.network.NetworkModule
import com.tyrads.sdk.userbase.network.TyradsApiService
import com.tyrads.sdk.userbase.network.TyradsHttpError
import com.tyrads.sdk.userbase.network.TyradsJson
import com.tyrads.sdk.userbase.network.TyradsRepository
import com.tyrads.sdk.userbase.session.SessionStore
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

/** Covers how transport failures are classified and how secure mode reshapes the request body. */
class TyradsRepositoryTest {

    private lateinit var api: TyradsApiService
    private lateinit var networkModule: NetworkModule
    private lateinit var sessionStore: SessionStore
    private lateinit var repository: TyradsRepository
    private val sentBodies = mutableListOf<String>()

    @Before
    fun setUp() {
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg())
        }

        api = mockk()
        sessionStore = mockk(relaxed = true)
        every { sessionStore.isSecure } returns false
        every { sessionStore.currentLanguage } returns "en-US"

        networkModule = mockk()
        every { networkModule.api } returns api
        every { networkModule.json } returns TyradsJson
        val body = slot<String>()
        every { networkModule.jsonBody(capture(body)) } answers {
            sentBodies += body.captured
            body.captured.toRequestBody("application/json".toMediaType())
        }

        repository = TyradsRepository(networkModule, sessionStore)
    }

    @After
    fun tearDown() {
        unmockkStatic(Base64::class)
    }

    @Test
    fun `socket timeout is reported as a timeout, not a generic failure`() = runTest {
        coEvery { api.getCampaigns(any(), any()) } throws SocketTimeoutException("timed out")

        assertThrows(TyradsHttpError.Timeout::class.java) {
            runBlockingCatching { repository.getCampaigns("en-US") }
        }
    }

    @Test
    fun `io failures are reported as network errors`() = runTest {
        coEvery { api.getCampaigns(any(), any()) } throws IOException("no route to host")

        assertThrows(TyradsHttpError.Network::class.java) {
            runBlockingCatching { repository.getCampaigns("en-US") }
        }
    }

    @Test
    fun `non-2xx responses keep the status code and raw error body`() = runTest {
        val errorJson = """{"code":401,"message":"[ES1006] Invalid API Key / API Secret","errorCode":"ES1006"}"""
        coEvery { api.getCampaigns(any(), any()) } returns
            Response.error(401, errorJson.toResponseBody("application/json".toMediaType()))

        val error = assertThrows(TyradsHttpError.Server::class.java) {
            runBlockingCatching { repository.getCampaigns("en-US") }
        }

        assertEquals(401, error.status)
        assertTrue(error.body!!.contains("ES1006"))
    }

    @Test
    fun `unexpected failures surface as unknown`() = runTest {
        coEvery { api.getCampaigns(any(), any()) } throws IllegalStateException("boom")

        assertThrows(TyradsHttpError.Unknown::class.java) {
            runBlockingCatching { repository.getCampaigns("en-US") }
        }
    }

    @Test
    fun `campaigns unwraps the data array`() = runTest {
        coEvery { api.getCampaigns(any(), any()) } returns Response.success(
            TyradsOffersResponse(
                code = 200,
                data = listOf(campaign()),
                message = "OK",
                timestamp = 1L,
                responseTime = 1.5,
            ),
        )

        val campaigns = repository.getCampaigns("en-US")

        assertEquals(1, campaigns.size)
        assertEquals(4785, campaigns.first().campaignId)
    }

    @Test
    fun `activated summary unwraps the count`() = runTest {
        coEvery { api.getActivatedSummary(any()) } returns
            Response.success(ActivatedSummaryResponse(ActivatedSummaryData(activeCampaignCount = 7)))

        assertEquals(7, repository.getActivatedSummary("en-US"))
    }

    @Test
    fun `engagement returns null when no currency sale is running`() = runTest {
        coEvery { api.getEngagement(any()) } returns
            Response.success(EngagementResponse(EngagementData(CurrencySales = null)))

        assertNull(repository.getEngagement("en-US"))
    }

    @Test
    fun `engagement unwraps an active currency sale`() = runTest {
        coEvery { api.getEngagement(any()) } returns Response.success(
            EngagementResponse(EngagementData(CurrencySales = CurrencySales(name = "Double Coins"))),
        )

        assertEquals("Double Coins", repository.getEngagement("en-US")?.name)
    }

    @Test
    fun `plain mode sends the activity payload as readable json`() = runTest {
        coEvery { api.trackActivity(any()) } returns Response.success(TyradsJson.parseToJsonElement("{}"))

        repository.trackActivity("Opened")

        assertEquals("""{"activity":"Opened"}""", sentBodies.single())
    }

    @Test
    fun `secure mode replaces the payload with an encrypted envelope`() = runTest {
        every { sessionStore.isSecure } returns true
        every { sessionStore.encKey } returns "test-only-32-byte-encryption-key"
        coEvery { api.trackActivity(any()) } returns Response.success(TyradsJson.parseToJsonElement("{}"))

        repository.trackActivity("Opened")

        val sent = sentBodies.single()
        assertTrue("plaintext must not leak", !sent.contains("Opened"))
        assertTrue(sent.contains("\"val\""))
        assertTrue(sent.contains("\"vec\""))
        assertTrue(sent.contains("\"tag\""))
    }

    private fun campaign() = Campaign(
        campaignId = 4785,
        tracking = CampaignTracking(),
        packageName = "id6745805828",
        os = "iOS",
        title = "Woody Block Color Blast",
    )

    /** `assertThrows` needs a non-suspending block; the repository dispatches internally anyway. */
    private fun <T> runBlockingCatching(block: suspend () -> T): T =
        kotlinx.coroutines.runBlocking { block() }
}
