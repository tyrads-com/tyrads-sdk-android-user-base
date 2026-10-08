package com.tyrads.sdk.userbase.push

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.tyrads.sdk.userbase.config.TyradsConfig
import com.tyrads.sdk.userbase.util.Logger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Fetches an FCM token scoped to TyrAds' own Firebase project (see [FcmConfig]), sent as
 * `devicePushToken` on login. The host app is never involved: it needs no Firebase setup and never
 * sees this token. Mirrors the RN User Base SDK's android-fcm.ts + TyradsFcmModule.kt.
 *
 * Uses a separately named [FirebaseApp] so it coexists with whatever default Firebase app the host
 * does (or doesn't) have. `FirebaseMessaging.getInstance(FirebaseApp)` isn't public, so the
 * instance comes from the generic `FirebaseApp.get(Class)` component lookup instead.
 */
internal object TyradsPushToken {

    private const val APP_NAME = "TyradsUserBaseFcm"
    private const val TOKEN_TIMEOUT_MS = 10_000L

    private val mutex = Mutex()
    @Volatile private var cachedToken: String? = null
    @Volatile private var permissionRequested = false

    /** Never throws. Returns null if Firebase/Play Services can't produce a token in time. */
    suspend fun get(context: Context): String? {
        cachedToken?.let { return it }
        return mutex.withLock {
            cachedToken ?: fetch(context.applicationContext)?.also { cachedToken = it }
        }
    }

    private suspend fun fetch(context: Context): String? = try {
        requestNotificationPermissionIfNeeded(context)
        val app = FirebaseApp.getApps(context).firstOrNull { it.name == APP_NAME }
            ?: FirebaseApp.initializeApp(context, FcmConfig.firebaseOptions(TyradsConfig.environment), APP_NAME)
        val messaging = app.get(FirebaseMessaging::class.java)
        withTimeoutOrNull(TOKEN_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                messaging.token
                    .addOnSuccessListener { continuation.resume(it) }
                    .addOnFailureListener {
                        Logger.w("TyrAds FCM token fetch failed", it)
                        continuation.resume(null)
                    }
            }
        } ?: run {
            Logger.w("TyrAds FCM token unavailable, logging in without devicePushToken")
            null
        }
    } catch (t: Throwable) {
        Logger.w("TyrAds FCM token fetch failed", t)
        null
    }

    /**
     * API 33+: asks for POST_NOTIFICATIONS once per process, like the RN SDK does on login.
     * Fire-and-forget through [NotificationPermissionActivity], since a token can be fetched without
     * the permission. It only decides whether notifications actually display.
     */
    private fun requestNotificationPermissionIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || permissionRequested) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) return
        permissionRequested = true
        try {
            context.startActivity(
                Intent(context, NotificationPermissionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (t: Throwable) {
            Logger.w("Couldn't request POST_NOTIFICATIONS", t)
        }
    }
}
