package com.tyrads.sdk.userbase.device

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.WindowManager
import java.util.TimeZone
import kotlin.math.roundToInt

/** Telephony, CPU, memory, screen, network and clock fields of the device-data payload. */
internal object TrackingInfo {

    private fun telephonyManager(context: Context): TelephonyManager? =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    fun getCarrierName(context: Context): String = telephonyManager(context)?.networkOperatorName ?: "Unknown"

    fun getMccMnc(context: Context): String = telephonyManager(context)?.networkOperator ?: "Unknown"

    fun getMcc(context: Context): String {
        val mccMnc = getMccMnc(context)
        return if (mccMnc.length >= 3) mccMnc.substring(0, 3) else "Unknown"
    }

    fun getMnc(context: Context): String {
        val mccMnc = getMccMnc(context)
        return if (mccMnc.length > 3) mccMnc.substring(3) else "Unknown"
    }

    fun getCountryIso(context: Context): String = telephonyManager(context)?.networkCountryIso ?: "Unknown"

    fun isRoaming(context: Context): String = (telephonyManager(context)?.isNetworkRoaming ?: false).toString()

    fun getSimOperatorName(context: Context): String = telephonyManager(context)?.simOperatorName ?: "Unknown"

    fun getSimOperator(context: Context): String = telephonyManager(context)?.simOperator ?: "Unknown"

    fun getSimCountryIso(context: Context): String = telephonyManager(context)?.simCountryIso ?: "Unknown"

    fun getPhoneType(context: Context): String = when (telephonyManager(context)?.phoneType) {
        TelephonyManager.PHONE_TYPE_GSM -> "GSM"
        TelephonyManager.PHONE_TYPE_CDMA -> "CDMA"
        TelephonyManager.PHONE_TYPE_SIP -> "SIP"
        TelephonyManager.PHONE_TYPE_NONE -> "None"
        else -> "Unknown"
    }

    fun getSupportedAbis(): String = Build.SUPPORTED_ABIS.joinToString(",").ifEmpty { "unknown" }

    fun getCpuType(): String = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"

    fun getCpuCores(): String = Runtime.getRuntime().availableProcessors().toString()

    fun getCpuModel(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL ?: "Unknown" else "Unknown"

    fun getOsArch(): String? = System.getProperty("os.arch")

    fun getMaxMemoryMb(): String = (Runtime.getRuntime().maxMemory() / 1024 / 1024).toString()

    fun getFreeMemoryMb(): String = (Runtime.getRuntime().freeMemory() / 1024 / 1024).toString()

    fun getSupported32BitAbis(): String = Build.SUPPORTED_32_BIT_ABIS.joinToString(",")

    fun getSupported64BitAbis(): String = Build.SUPPORTED_64_BIT_ABIS.joinToString(",")

    fun getTotalMemoryBytes(context: Context): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 0L
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.totalMem
    }

    /** Full physical screen size in density-independent pixels (dp), rounded. */
    fun getScreenWidthDp(context: Context): Int = realMetrics(context).let { (it.widthPixels / it.density).roundToInt() }

    fun getScreenHeightDp(context: Context): Int = realMetrics(context).let { (it.heightPixels / it.density).roundToInt() }

    fun getScreenDensity(context: Context): Float = realMetrics(context).density

    private fun realMetrics(context: Context): DisplayMetrics {
        val metrics = DisplayMetrics()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        return metrics
    }

    /** `none` / `bluetooth` / `cellular` / `ethernet` / `wifi` / `vpn` / `unknown`. */
    fun getConnectionType(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return "unknown"
        val network = cm.activeNetwork ?: return "none"
        val caps = cm.getNetworkCapabilities(network) ?: return "unknown"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "bluetooth"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "vpn"
            else -> "unknown"
        }
    }

    /** True whenever a VPN transport is present, even when it rides on top of wifi/cellular. */
    fun isVpnActive(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }

    fun getNetworkSpeed(context: Context): String {
        val kbps = try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            (cm.getNetworkCapabilities(cm.activeNetwork)?.linkDownstreamBandwidthKbps ?: 0) / 8
        } catch (e: Exception) {
            0
        }
        return "$kbps KB/s"
    }

    fun getTimeZoneId(): String = TimeZone.getDefault().id

    /** Current UTC offset in minutes, including daylight saving time. */
    fun getTimeZoneOffsetMinutes(): Int = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000

    fun getSystemTime(): Long = System.currentTimeMillis()
}
