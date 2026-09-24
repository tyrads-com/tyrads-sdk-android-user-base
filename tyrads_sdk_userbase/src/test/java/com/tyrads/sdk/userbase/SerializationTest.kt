package com.tyrads.sdk.userbase

import com.tyrads.sdk.userbase.models.ActivatedSummaryResponse
import com.tyrads.sdk.userbase.models.Campaign
import com.tyrads.sdk.userbase.models.EngagementResponse
import com.tyrads.sdk.userbase.models.TyradsDeviceData
import com.tyrads.sdk.userbase.models.TyradsInitRequest
import com.tyrads.sdk.userbase.models.TyradsInitResponse
import com.tyrads.sdk.userbase.models.TyradsOffersResponse
import com.tyrads.sdk.userbase.models.TyradsSingleResponse
import com.tyrads.sdk.userbase.network.TyradsJson
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parses payloads in the exact shape the production API returns. The envelope's `responseTime` is
 * fractional and `account/engagement` nests `CurrencySales` one level deeper than the other
 * endpoints — both have broken parsing before, so they're pinned here.
 */
class SerializationTest {

    private val json: Json = TyradsJson

    @Test
    fun `init response parses fractional responseTime`() {
        val payload = """
            {
              "code": 200,
              "message": "OK",
              "timestamp": 1790171472756,
              "responseTime": 507.1244530007243,
              "data": {
                "newRegisteredUser": false,
                "newRegisteredDevice": false,
                "accountInfo": { "id": 47152543, "publisherUserId": "test_user_1" },
                "appInfo": { "headerColor": "#000F1E", "mainColor": "#b32da7", "premiumColor": "#7715A6" },
                "token": "ey.jwt.token"
              }
            }
        """.trimIndent()

        val parsed = json.decodeFromString(TyradsInitResponse.serializer(), payload)

        assertEquals(200, parsed.code)
        assertEquals(507.1244530007243, parsed.responseTime, 0.0000001)
        assertEquals(1790171472756L, parsed.timestamp)
        assertEquals("test_user_1", parsed.data.accountInfo.publisherUserId)
        assertEquals(47152543L, parsed.data.accountInfo.id)
        assertEquals("ey.jwt.token", parsed.data.token)
        assertFalse(parsed.data.newRegisteredUser)
    }

    @Test
    fun `campaign list parses the flat userbase shape`() {
        val parsed = json.decodeFromString(
            TyradsOffersResponse.serializer(Campaign.serializer()),
            CAMPAIGN_LIST_PAYLOAD,
        )

        assertEquals(1, parsed.data.size)
        assertEquals(2, parsed.meta?.itemCount)

        val campaign = parsed.data.first()
        assertEquals(4785, campaign.campaignId)
        assertEquals("Woody Block Color Blast", campaign.title)
        assertEquals("id6745805828", campaign.packageName)
        assertEquals("iOS", campaign.os)
        assertNull(campaign.tracking.s2sClickUrl)
        assertTrue(campaign.tracking.clickUrl!!.startsWith("https://"))
        assertEquals(1, campaign.currencies.size)
        assertEquals("Belanda Coins", campaign.currencies.first().currencyName)
        assertEquals(30485.0, campaign.payoutSummary.getValue("2").totalPayoutConverted, 0.001)
        assertEquals(0.0, campaign.payoutSummary.getValue("2").totalMicrochargePayoutConverted, 0.001)
        assertNull(campaign.activeCurrencyId)
        assertNull(campaign.installedOn)

        val event = campaign.events.single()
        assertEquals(24987, event.appEventId)
        assertEquals("Reach level 3", event.eventName)
        assertEquals("Playable", event.type)
        assertEquals(30, event.maxTime)
        assertEquals("days", event.maxTimeMetric)
        assertEquals(105.0, event.payoutInfo.getValue("2").payoutAmountConverted, 0.001)
        assertTrue(event.lockEventRule.isEmpty())
        assertNull(event.count)
    }

    @Test
    fun `campaign detail parses as a single object, not a list`() {
        val payload = """
            {
              "code": 200,
              "data": { "campaignId": 4785, "packageName": "id6745805828", "os": "iOS",
                        "title": "Woody Block Color Blast" },
              "message": "OK",
              "timestamp": 1790171713106,
              "responseTime": 889.0411169975996
            }
        """.trimIndent()

        val parsed = json.decodeFromString(TyradsSingleResponse.serializer(Campaign.serializer()), payload)

        assertEquals(4785, parsed.data.campaignId)
        assertEquals("Woody Block Color Blast", parsed.data.title)
    }

    @Test
    fun `campaign tolerates absent optional fields`() {
        val minimal = """{ "campaignId": 1, "packageName": "com.x", "os": "Android", "title": "X" }"""

        val campaign = json.decodeFromString(Campaign.serializer(), minimal)

        assertNull(campaign.thumbnail)
        assertNull(campaign.creativeUrl)
        assertTrue(campaign.events.isEmpty())
        assertTrue(campaign.currencies.isEmpty())
        assertTrue(campaign.payoutSummary.isEmpty())
    }

    @Test
    fun `unknown fields are ignored`() {
        val withExtras =
            """{ "campaignId": 1, "packageName": "com.x", "os": "Android", "title": "X",
                 "somethingNewFromBackend": { "nested": true } }"""

        val campaign = json.decodeFromString(Campaign.serializer(), withExtras)

        assertEquals(1, campaign.campaignId)
    }

    @Test
    fun `engagement unwraps a null CurrencySales`() {
        val payload = """
            { "code": 200, "data": { "CurrencySales": null }, "message": "OK",
              "timestamp": 1790171716238, "responseTime": 30.58 }
        """.trimIndent()

        val parsed = json.decodeFromString(EngagementResponse.serializer(), payload)

        assertNull(parsed.data.CurrencySales)
    }

    @Test
    fun `engagement unwraps an active CurrencySales`() {
        val payload = """
            { "code": 200,
              "data": { "CurrencySales": { "name": "Double Coins", "multiplier": 2.0,
                                            "bannerUrl": "https://x/y.png", "dateStart": "2026-09-01",
                                            "dateEnd": "2026-09-30", "remainingTimeSeconds": 86400 } },
              "message": "OK", "timestamp": 1, "responseTime": 1.5 }
        """.trimIndent()

        val sale = json.decodeFromString(EngagementResponse.serializer(), payload).data.CurrencySales

        assertEquals("Double Coins", sale?.name)
        assertEquals(2.0, sale?.multiplier!!, 0.001)
        assertEquals(86400L, sale.remainingTimeSeconds)
    }

    @Test
    fun `activated summary unwraps the campaign count`() {
        val payload = """
            { "code": 200, "data": { "activeCampaignCount": 3 }, "message": "OK",
              "timestamp": 1, "responseTime": 1.0 }
        """.trimIndent()

        val parsed = json.decodeFromString(ActivatedSummaryResponse.serializer(), payload)

        assertEquals(3, parsed.data.activeCampaignCount)
    }

    @Test
    fun `login request flattens media source and user info, omitting nulls`() {
        val request = TyradsInitRequest(
            publisherUserId = "user_1",
            platform = "Android",
            deviceData = deviceData(),
            identifierType = "GAID",
            identifier = "a2ef9d2c-6bea-4f84-8439-08607aabf060",
            devicePushToken = "fcm-token",
            mediaSourceName = "Facebook",
            sub3 = "HighValueUser",
            email = "user@example.com",
            gender = 1,
        )

        val encoded = json.parseToJsonElement(
            json.encodeToString(TyradsInitRequest.serializer(), request),
        ) as JsonObject

        assertEquals("user_1", encoded["publisherUserId"]?.jsonPrimitive?.content)
        assertEquals("GAID", encoded["identifierType"]?.jsonPrimitive?.content)
        assertEquals("fcm-token", encoded["devicePushToken"]?.jsonPrimitive?.content)
        // media source / user info are flat on the payload, not nested objects
        assertEquals("Facebook", encoded["mediaSourceName"]?.jsonPrimitive?.content)
        assertEquals("HighValueUser", encoded["sub3"]?.jsonPrimitive?.content)
        assertEquals("user@example.com", encoded["email"]?.jsonPrimitive?.content)
        assertEquals(1, encoded["gender"]?.jsonPrimitive?.content?.toInt())
        // unset optionals must not be sent at all
        assertFalse(encoded.containsKey("engagementId"))
        assertFalse(encoded.containsKey("sub1"))
        assertFalse(encoded.containsKey("phoneNumber"))
        assertTrue(encoded.containsKey("deviceData"))
    }

    @Test
    fun `device data serializes package under its wire name`() {
        val encoded = json.parseToJsonElement(
            json.encodeToString(TyradsDeviceData.serializer(), deviceData()),
        ) as JsonObject

        assertEquals("com.example.host", encoded["package"]?.jsonPrimitive?.content)
        assertFalse(encoded.containsKey("packageName"))
        // behavioral counters are always zero
        assertEquals(0, encoded["keyboardNumEvents"]?.jsonPrimitive?.content?.toInt())
        assertEquals(0, encoded["touchNumEvents"]?.jsonPrimitive?.content?.toInt())
    }

    @Test
    fun `device data matches the shared user-base payload shape`() {
        val encoded = json.parseToJsonElement(
            json.encodeToString(TyradsDeviceData.serializer(), deviceData()),
        ) as JsonObject

        // string-typed on the wire, not numbers or arrays
        listOf("supportedAbis", "cpuCores", "androidSdkInt", "build", "version", "maxMemory", "freeMemory")
            .forEach { key -> assertTrue("$key must be a string", encoded.getValue(key).jsonPrimitive.isString) }
        // number-typed on the wire
        listOf("systemTime", "totalMemory", "screenWidth", "screenHeight", "timeZoneOffset", "deviceAge")
            .forEach { key -> assertFalse("$key must be a number", encoded.getValue(key).jsonPrimitive.isString) }
        // fields the other SDKs don't send
        listOf(
            "usedMemory", "bluetoothSupported", "bluetoothLESupported", "bluetoothAdapterName",
            "touchMultitouch", "touchMultitouchDistinct", "touchMultitouchJazzhand", "touchPressure",
        ).forEach { key -> assertFalse("$key must not be sent", encoded.containsKey(key)) }
    }

    @Test
    fun `campaign list survives an empty data array`() {
        val payload =
            """{ "code": 200, "data": [], "meta": { "itemCount": 0 }, "message": "OK",
                 "timestamp": 1, "responseTime": 1.0 }"""

        val parsed = json.decodeFromString(
            TyradsOffersResponse.serializer(Campaign.serializer()),
            payload,
        )

        assertTrue(parsed.data.isEmpty())
    }

    @Test
    fun `campaign list serializer round-trips through the list serializer used by callers`() {
        val campaigns = json.decodeFromString(
            TyradsOffersResponse.serializer(Campaign.serializer()),
            CAMPAIGN_LIST_PAYLOAD,
        ).data

        val reEncoded = json.encodeToString(ListSerializer(Campaign.serializer()), campaigns)

        assertTrue(reEncoded.contains("\"campaignId\":4785"))
    }

    private fun deviceData() = TyradsDeviceData(
        deviceId = "b879d99ba2fc3998",
        device = "phone",
        brand = "google",
        model = "sdk_gphone16k_arm64",
        manufacturer = "Google",
        product = "b879d99ba2fc3998",
        fingerprint = "google/sdk/emu:17/CP21/15181570:user/dev-keys",
        baseOs = "Android",
        releaseVersion = "17",
        androidApiLevel = 37,
        build = "1",
        version = "1.0",
        packageName = "com.example.host",
        apiVersion = "4.0",
        platform = "Android",
        installerStore = "com.android.shell",
        installerPackageName = "com.android.shell",
        osLang = "en-US",
        rooted = false,
        virtual = true,
        sdkVersion = "1.0.0-0",
        sdkPlatform = "Android",
        carrierName = "T-Mobile",
        supportedAbis = "arm64-v8a",
        cpuType = "arm64-v8a",
        totalMemory = 4160389120L,
        screenWidth = 448,
        screenHeight = 997,
        screenDensity = 3.0f,
        connectionType = "wifi",
        isVpnActive = false,
        timeZone = "Asia/Jakarta",
        timeZoneOffset = 420,
        systemTime = 1790225728940L,
        gpu = "ranchu",
        bluetooth = "BLE_SUPPORTED",
        touchSupport = "MULTITOUCH_JAZZHAND",
        googleAppSetID = "95e62a3f-1c89-05a5-fbf7-ab2319eb9e04",
        deviceUpTime = "0",
        deviceBootTime = "Wed Sep 23 20:15:07 GMT+07:00 2026",
        mcc = "310",
        mnc = "260",
        mccMnc = "310260",
        countryIso = "us",
        isRoaming = "false",
        simOperatorName = "T-Mobile",
        simOperator = "310260",
        simCountryIso = "us",
        phoneType = "GSM",
        host = "60da2e291b03",
        tags = "dev-keys",
        type = "user",
        codename = "REL",
        buildType = "user",
        buildTags = "dev-keys",
        buildSign = "a133618caa3089d9",
        hardware = "ranchu",
        androidId = "b879d99ba2fc3998",
        serialNumber = "unknown",
        deviceAge = 1786003129877L,
        cpuCores = "4",
        cpuHardware = "ranchu",
        cpuModel = "ranchu",
        osArch = "aarch64",
        maxMemory = "192",
        freeMemory = "0",
        supported32BitAbis = "",
        supported64BitAbis = "arm64-v8a",
        deviceManufacturer = "Google",
        deviceModel = "sdk_gphone16k_arm64",
        deviceBrand = "google",
        deviceBoard = "goldfish_arm64",
        deviceHardware = "ranchu",
        androidVersion = "17",
        androidSdkInt = "37",
        networkSpeed = "3750 KB/s",
    )

    private companion object {
        const val CAMPAIGN_LIST_PAYLOAD = """
            {
              "code": 200,
              "data": [
                {
                  "campaignId": 4785,
                  "tracking": {
                    "impressionUrl": "https://www.ltv-mob.com/i/MJWMX2/2M3X9PJB/?source_id=RP0000003",
                    "clickUrl": "https://www.ltv-mob.com/MJWMX2/2M3X9PJB/?source_id=RP0000003",
                    "s2sClickUrl": null
                  },
                  "packageName": "id6745805828",
                  "os": "iOS",
                  "title": "Woody Block Color Blast",
                  "thumbnail": "https://is1-ssl.mzstatic.com/image/thumb/x/512x512bb.jpg",
                  "creativeUrl": ".",
                  "campaignDescription": "1. Play and enjoy.",
                  "installedOn": null,
                  "activatedOn": null,
                  "uninstalledOn": null,
                  "expiredOn": null,
                  "expiredInSeconds": null,
                  "currencies": [
                    { "currencyId": 2, "currencyName": "Belanda Coins",
                      "currencyIcon": "https://tyrads-public-files.s3.amazonaws.com/coin.png" }
                  ],
                  "activeCurrencyId": null,
                  "payoutSummary": {
                    "2": { "totalPayoutConverted": 30485, "totalPlayablePayoutConverted": 30485,
                           "totalMicrochargePayoutConverted": 0 }
                  },
                  "earnedPayout": {},
                  "stage": null,
                  "engagements": [],
                  "events": [
                    {
                      "appEventId": 24987,
                      "identifier": "event_identifier_2",
                      "eventName": "Reach level 3",
                      "eventDescription": null,
                      "allowDuplicateEvents": false,
                      "payoutInfo": { "2": { "payoutAmountConverted": 105 } },
                      "rewardedOn": null,
                      "conversionStatus": null,
                      "lockEventRule": [],
                      "hideEventRule": [],
                      "shorterMaxTimeRule": null,
                      "isTicketSubmitted": false,
                      "ticketStatus": null,
                      "ticketUrl": null,
                      "ticketRejectReason": null,
                      "ticketRejectionCode": null,
                      "count": null,
                      "limit": null,
                      "maxTime": 30,
                      "maxTimeMetric": "days",
                      "maxTimeRemainSeconds": null,
                      "enforceMaxTimeCompletion": true,
                      "rewardingExpiredOn": null,
                      "rewardingExpiredInSeconds": null,
                      "type": "Playable"
                    }
                  ]
                }
              ],
              "meta": { "itemCount": 2 },
              "message": "OK",
              "timestamp": 1790171664722,
              "responseTime": 706.5777099989355
            }
        """
    }
}
