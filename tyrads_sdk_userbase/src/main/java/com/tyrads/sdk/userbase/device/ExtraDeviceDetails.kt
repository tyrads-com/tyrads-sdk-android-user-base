package com.tyrads.sdk.userbase.device

import android.content.Context
import android.content.pm.PackageManager
import android.opengl.GLES20
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import com.google.android.gms.appset.AppSet
import com.google.android.gms.tasks.Tasks
import com.tyrads.sdk.userbase.util.Logger
import java.security.MessageDigest
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext

/** GPU, bluetooth, touch, identity and uptime fields of the device-data payload. */
internal object ExtraDeviceDetails {

    fun getGpu(): String = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL ?: "Unknown"
        } else {
            val renderer = getOpenGLRenderer()
            if (!renderer.isNullOrEmpty() && renderer != "Unknown") renderer else getGpuFromSystemProperty()
        }
    } catch (e: Exception) {
        "Unknown"
    }

    /** Reads the GL renderer string from a throwaway 1x1 pbuffer context. */
    private fun getOpenGLRenderer(): String? {
        return try {
            val egl = EGLContext.getEGL() as? EGL10 ?: return null
            val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
            if (display == EGL10.EGL_NO_DISPLAY) return null
            if (!egl.eglInitialize(display, IntArray(2))) return null

            val configAttribs = intArrayOf(
                EGL10.EGL_RENDERABLE_TYPE, 4,
                EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
                EGL10.EGL_NONE,
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            if (!egl.eglChooseConfig(display, configAttribs, configs, 1, numConfigs) ||
                configs[0] == null || numConfigs[0] == 0
            ) {
                egl.eglTerminate(display)
                return null
            }

            val eglContextClientVersion = 0x3098
            val context = egl.eglCreateContext(
                display,
                configs[0],
                EGL10.EGL_NO_CONTEXT,
                intArrayOf(eglContextClientVersion, 2, EGL10.EGL_NONE),
            )
            if (context == EGL10.EGL_NO_CONTEXT) {
                egl.eglTerminate(display)
                return null
            }

            val surface = egl.eglCreatePbufferSurface(
                display,
                configs[0],
                intArrayOf(EGL10.EGL_WIDTH, 1, EGL10.EGL_HEIGHT, 1, EGL10.EGL_NONE),
            )
            if (surface == EGL10.EGL_NO_SURFACE) {
                egl.eglDestroyContext(display, context)
                egl.eglTerminate(display)
                return null
            }

            egl.eglMakeCurrent(display, surface, surface, context)
            val renderer = GLES20.glGetString(GLES20.GL_RENDERER)
            val vendor = GLES20.glGetString(GLES20.GL_VENDOR)
            val glVersion = GLES20.glGetString(GLES20.GL_VERSION)

            egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)
            egl.eglDestroySurface(display, surface)
            egl.eglDestroyContext(display, context)
            egl.eglTerminate(display)

            when {
                !renderer.isNullOrEmpty() -> renderer
                !vendor.isNullOrEmpty() -> vendor
                !glVersion.isNullOrEmpty() -> glVersion
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getGpuFromSystemProperty(): String = try {
        readSystemProperty("ro.hardware.gles")
    } catch (e: Exception) {
        try {
            readSystemProperty("ro.opengles.version")
        } catch (e2: Exception) {
            "Unknown"
        }
    }

    private fun readSystemProperty(name: String): String {
        val process = Runtime.getRuntime().exec(arrayOf("getprop", name))
        val result = process.inputStream.bufferedReader().readLine() ?: "Unknown"
        process.destroy()
        return result
    }

    fun getBluetoothInfo(context: Context): String {
        val pm = context.packageManager
        return when {
            pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) -> "BLE_SUPPORTED"
            pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) -> "CLASSIC_SUPPORTED"
            else -> "NOT_SUPPORTED"
        }
    }

    fun getTouchSupport(context: Context): String {
        val pm = context.packageManager
        return when {
            pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_JAZZHAND) -> "MULTITOUCH_JAZZHAND"
            pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT) -> "MULTITOUCH_DISTINCT"
            pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH) -> "MULTITOUCH"
            pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN) -> "SINGLE_TOUCH"
            else -> "NOT_SUPPORTED"
        }
    }

    fun getGoogleAppSetIdSync(context: Context): String = try {
        val info = Tasks.await(AppSet.getClient(context).appSetIdInfo, 3, TimeUnit.SECONDS)
        info.id.ifEmpty { "Unknown" }
    } catch (t: Throwable) {
        Logger.w("Failed to fetch Google App Set ID", t)
        "Unknown"
    }

    fun getDeviceUpTimeHours(): String = (SystemClock.uptimeMillis() / (1000 * 60 * 60)).toString()

    fun getDeviceBootTime(): String = Date(System.currentTimeMillis() - SystemClock.uptimeMillis()).toString()

    fun getInstallerPackageName(context: Context): String = try {
        val pm = context.packageManager
        val name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pm.getInstallSourceInfo(context.packageName).initiatingPackageName
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(context.packageName)
        }
        name ?: "unknown"
    } catch (t: Throwable) {
        "unknown"
    }

    fun getAndroidId(context: Context): String? = try {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    } catch (t: Throwable) {
        null
    }

    fun getSerialNumber(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            Build.getSerial()
        } catch (e: SecurityException) {
            Build.UNKNOWN
        }
    } else {
        @Suppress("DEPRECATION")
        Build.SERIAL
    }

    /** SHA-256 of each APK signing certificate, uppercase hex, comma-joined; null if unreadable. */
    fun getBuildSign(context: Context): String? = try {
        val pm = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo?.apkContentsSigners?.joinToString { hashSignature(it.toByteArray()) }
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                .signatures?.joinToString { hashSignature(it.toByteArray()) }
        }
    } catch (e: Exception) {
        null
    }

    private fun hashSignature(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02X".format(it) }

    /**
     * Proxy for the device's manufacture date: the `firstInstallTime` shared by the most installed
     * packages (OEM system apps are bulk-installed at once), earliest one wins.
     */
    fun getDeviceAge(context: Context): Long? = try {
        val now = Date().time
        val counts = mutableMapOf<Long, Int>()
        var highestCount = 1
        context.packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES).forEach { pkg ->
            val t = pkg.firstInstallTime
            if (t > 1293840000000 && t <= now) {
                counts[t] = (counts[t] ?: 0) + 1
                highestCount = maxOf(highestCount, counts[t] ?: 0)
            }
        }
        counts.filterValues { it == highestCount }.keys.minOrNull()
    } catch (e: Exception) {
        null
    }
}
