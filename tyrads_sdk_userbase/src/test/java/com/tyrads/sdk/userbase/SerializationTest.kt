package com.tyrads.sdk.userbase

import com.tyrads.sdk.userbase.models.TyradsDeviceData
import com.tyrads.sdk.userbase.models.TyradsInitRequest
import com.tyrads.sdk.userbase.network.TyradsJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the wire shape of what the SDK sends. Responses aren't modeled (they're handed back as raw
 * JSON), so only request payloads are covered here.
 */
class SerializationTest {

    private val json: Json = TyradsJson

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
        sdkPlatform = "Android-userbase",
        carrierName = "T-Mobile",
        supportedAbis = "arm64-v8a",
        cpuType = "arm64-v8a",
        totalMemory = 128.0,
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

}
