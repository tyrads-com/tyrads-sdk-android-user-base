package com.tyrads.sdk.userbase.device

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.scottyab.rootbeer.RootBeer
import com.tyrads.sdk.userbase.config.TyradsConfig
import com.tyrads.sdk.userbase.models.TyradsDeviceData
import com.tyrads.sdk.userbase.session.SessionStore
import com.tyrads.sdk.userbase.util.Logger
import com.tyrads.sdk.userbase.util.UuidGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

internal data class AdvertisingIdentity(val identifierType: String, val identifier: String)

/**
 * Assembles the full `deviceData` payload sent on `initialize`, and resolves the GAID (falling
 * back to a locally generated+persisted UUID when unavailable/limited).
 */
internal object DeviceDataCollector {

    suspend fun collect(context: Context): TyradsDeviceData = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val androidId = ExtraDeviceDetails.getAndroidId(app)
        val packageInfo = app.packageManager.getPackageInfo(app.packageName, 0)
        val installer = ExtraDeviceDetails.getInstallerPackageName(app)
        val connectionType = TrackingInfo.getConnectionType(app)

        TyradsDeviceData(
            deviceId = androidId ?: "unknown",
            device = if (isTablet(app)) "tablet" else "phone",
            brand = Build.BRAND,
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            product = androidId ?: "unknown",
            fingerprint = Build.FINGERPRINT,
            baseOs = "Android",
            releaseVersion = Build.VERSION.RELEASE,
            androidApiLevel = Build.VERSION.SDK_INT,
            build = PackageInfoCompat.getLongVersionCode(packageInfo).toString(),
            version = packageInfo.versionName ?: "unknown",
            packageName = app.packageName,
            apiVersion = TyradsConfig.API_VERSION,
            platform = "Android",
            installerStore = installer,
            installerPackageName = installer,
            osLang = getOsLang(app),
            rooted = isRooted(app),
            virtual = isEmulator(),
            sdkVersion = TyradsConfig.SDK_VERSION,
            sdkPlatform = TyradsConfig.SDK_PLATFORM,
            carrierName = TrackingInfo.getCarrierName(app),
            supportedAbis = TrackingInfo.getSupportedAbis(),
            cpuType = TrackingInfo.getCpuType(),
            totalMemory = TrackingInfo.getTotalStorageGb(),
            screenWidth = TrackingInfo.getScreenWidthDp(app),
            screenHeight = TrackingInfo.getScreenHeightDp(app),
            screenDensity = TrackingInfo.getScreenDensity(app),
            connectionType = connectionType,
            isVpnActive = TrackingInfo.isVpnActive(app),
            timeZone = TrackingInfo.getTimeZoneId(),
            timeZoneOffset = TrackingInfo.getTimeZoneOffsetMinutes(),
            systemTime = TrackingInfo.getSystemTime(),
            gpu = ExtraDeviceDetails.getGpu(),
            bluetooth = ExtraDeviceDetails.getBluetoothInfo(app),
            touchSupport = ExtraDeviceDetails.getTouchSupport(app),
            googleAppSetID = ExtraDeviceDetails.getGoogleAppSetIdSync(app),
            deviceUpTime = ExtraDeviceDetails.getDeviceUpTimeHours(),
            deviceBootTime = ExtraDeviceDetails.getDeviceBootTime(),
            mcc = TrackingInfo.getMcc(app),
            mnc = TrackingInfo.getMnc(app),
            mccMnc = TrackingInfo.getMccMnc(app),
            countryIso = TrackingInfo.getCountryIso(app),
            isRoaming = TrackingInfo.isRoaming(app),
            simOperatorName = TrackingInfo.getSimOperatorName(app),
            simOperator = TrackingInfo.getSimOperator(app),
            simCountryIso = TrackingInfo.getSimCountryIso(app),
            phoneType = TrackingInfo.getPhoneType(app),
            host = Build.HOST,
            tags = Build.TAGS,
            type = Build.TYPE,
            codename = Build.VERSION.CODENAME,
            buildType = Build.TYPE,
            buildTags = Build.TAGS ?: "Unknown",
            buildSign = ExtraDeviceDetails.getBuildSign(app),
            hardware = Build.HARDWARE,
            androidId = androidId,
            serialNumber = ExtraDeviceDetails.getSerialNumber(),
            deviceAge = ExtraDeviceDetails.getDeviceAge(app),
            cpuCores = TrackingInfo.getCpuCores(),
            cpuHardware = Build.HARDWARE,
            cpuModel = TrackingInfo.getCpuModel(),
            osArch = TrackingInfo.getOsArch(),
            maxMemory = TrackingInfo.getMaxMemoryMb(),
            freeMemory = TrackingInfo.getFreeMemoryMb(),
            supported32BitAbis = TrackingInfo.getSupported32BitAbis(),
            supported64BitAbis = TrackingInfo.getSupported64BitAbis(),
            deviceManufacturer = Build.MANUFACTURER,
            deviceModel = Build.MODEL,
            deviceBrand = Build.BRAND,
            deviceBoard = Build.BOARD,
            deviceHardware = Build.HARDWARE,
            androidVersion = Build.VERSION.RELEASE,
            androidSdkInt = Build.VERSION.SDK_INT.toString(),
            networkSpeed = TrackingInfo.getNetworkSpeed(app),
        )
    }

    /** GAID when available and not limited/blank; otherwise a locally generated+persisted UUID. */
    suspend fun resolveAdvertisingIdentity(context: Context, sessionStore: SessionStore): AdvertisingIdentity =
        withContext(Dispatchers.IO) {
            val info = try {
                AdvertisingIdClient.getAdvertisingIdInfo(context.applicationContext)
            } catch (e: GooglePlayServicesNotAvailableException) {
                Logger.w("Play services unavailable for GAID", e)
                null
            } catch (e: GooglePlayServicesRepairableException) {
                Logger.w("Play services needs repair for GAID", e)
                null
            } catch (e: IOException) {
                Logger.w("Failed to fetch GAID", e)
                null
            } catch (t: Throwable) {
                Logger.w("Unexpected error fetching GAID", t)
                null
            }

            val id = info?.id
            val isBlank = id.isNullOrBlank() ||
                info.isLimitAdTrackingEnabled ||
                id.replace("-", "").matches(Regex("^0+$"))

            if (!isBlank && id != null) {
                AdvertisingIdentity(identifierType = "GAID", identifier = id)
            } else {
                val fallback = sessionStore.getOrCreateCustomAdId { UuidGenerator.generate() }
                AdvertisingIdentity(identifierType = "OTHER", identifier = fallback)
            }
        }

    /** `{lang}-{COUNTRY}` locale format, e.g. `en-US`. */
    private fun getOsLang(context: Context): String = try {
        val locale = context.resources.configuration.locales[0]
        val country = locale.country
        if (country.isNotBlank()) "${locale.language}-$country" else locale.language
    } catch (t: Throwable) {
        "en-US"
    }

    private fun isTablet(context: Context): Boolean =
        (context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >=
            Configuration.SCREENLAYOUT_SIZE_LARGE

    private fun isRooted(context: Context): Boolean = try {
        RootBeer(context).isRooted
    } catch (t: Throwable) {
        false
    }

    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu") ||
            Build.PRODUCT.contains("sdk_google") ||
            Build.PRODUCT.contains("google_sdk") ||
            Build.PRODUCT.contains("sdk") ||
            Build.PRODUCT.contains("sdk_x86") ||
            Build.PRODUCT.contains("vbox86p") ||
            Build.BOARD == "QC_Reference_Phone" ||
            Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||
            "google_sdk" == Build.PRODUCT)
    }
}
