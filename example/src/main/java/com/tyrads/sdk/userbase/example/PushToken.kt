package com.tyrads.sdk.userbase.example

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

enum class PushTokenStatus { IDLE, FETCHING, FETCHED, UNAVAILABLE }

/**
 * Mirrors the RN example's `fetchDevicePushToken()` — lives in the EXAMPLE app, not the SDK
 * (the SDK never fetches a push token itself, see docs/initialization/push-notifications.md in
 * the RN repo). Requesting POST_NOTIFICATIONS (API 33+) is fire-and-forget: a real FCM token can
 * still be obtained without notification permission, it only gates whether notifications display.
 */
suspend fun fetchFcmToken(): String? = try {
    suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> continuation.resume(token) }
            .addOnFailureListener { continuation.resume(null) }
    }
} catch (t: Throwable) {
    null
}
