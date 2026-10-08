package com.tyrads.sdk.userbase.push

import com.google.firebase.FirebaseOptions
import com.tyrads.sdk.userbase.config.TyradsEnvironment

/**
 * TyrAds' own Firebase project config, one per environment, embedded so the SDK can fetch an FCM
 * token without the host app having any Firebase setup. Same projects/app IDs as the RN User Base
 * SDK's src/core/push/fcm-config.ts. XOR-obfuscated (see [SecurityUtils]) only to keep the values
 * out of a plain string scan of the AAR, the same approach the other TyrAds SDKs take.
 */
internal object FcmConfig {

    private class ObfuscatedFirebaseOptions(
        val projectId: ByteArray,
        val appId: ByteArray,
        val apiKey: ByteArray,
        val messagingSenderId: ByteArray,
        val storageBucket: ByteArray,
    )

    private val PRODUCTION = ObfuscatedFirebaseOptions(
        projectId = byteArrayOf(21, 23, 22, 1, 11, 2, 73, 30, 71, 16, 83, 23),
        appId = byteArrayOf(80, 84, 87, 68, 90, 93, 87, 20, 70, 70, 84, 71, 91, 88, 73, 4, 67, 2, 17, 2, 68, 80, 95, 86, 0, 15, 87, 64, 90, 91, 0, 78, 16, 67, 1, 23, 80, 88, 69, 7, 21, 81, 5, 9, 76),
        apiKey = byteArrayOf(32, 39, 30, 19, 60, 16, 37, 66, 44, 16, 9, 25, 59, 19, 57, 93, 29, 55, 17, 27, 127, 88, 19, 71, 34, 42, 37, 56, 60, 15, 87, 117, 1, 36, 21, 0, 18, 87, 67),
        messagingSenderId = byteArrayOf(82, 88, 81, 70, 92, 80, 87, 24, 68, 70, 92, 75),
        storageBucket = byteArrayOf(21, 23, 22, 1, 11, 2, 73, 30, 71, 16, 83, 23, 76, 7, 26, 23, 72, 4, 2, 30, 72, 71, 17, 88, 19, 15, 3, 23, 65, 8, 20, 93),
    )

    private val STAGING = ObfuscatedFirebaseOptions(
        projectId = byteArrayOf(21, 23, 22, 95, 28, 13, 15, 0, 6, 7, 4, 21, 11, 15, 20),
        appId = byteArrayOf(80, 84, 85, 69, 88, 92, 93, 28, 70, 64, 80, 74, 83, 85, 73, 4, 67, 2, 17, 2, 68, 80, 95, 7, 5, 89, 2, 16, 14, 11, 92, 75, 22, 23, 84, 67, 91, 3, 75, 80, 75, 94, 2, 93, 26),
        apiKey = byteArrayOf(32, 39, 30, 19, 60, 16, 38, 90, 65, 31, 35, 57, 40, 32, 49, 15, 26, 50, 54, 21, 126, 1, 55, 81, 62, 20, 10, 36, 10, 44, 44, 73, 35, 48, 80, 35, 53, 45, 38),
        messagingSenderId = byteArrayOf(80, 89, 83, 71, 86, 88, 87, 30, 64, 75, 84, 70),
        storageBucket = byteArrayOf(21, 23, 22, 95, 28, 13, 15, 0, 6, 7, 4, 21, 11, 15, 20, 75, 75, 15, 17, 8, 79, 85, 22, 82, 18, 26, 11, 0, 14, 14, 1, 3, 20, 3, 21),
    )

    fun firebaseOptions(env: TyradsEnvironment): FirebaseOptions {
        val fields = when (env) {
            TyradsEnvironment.PRODUCTION -> PRODUCTION
            TyradsEnvironment.STAGING -> STAGING
        }
        return FirebaseOptions.Builder()
            .setProjectId(SecurityUtils.deobfuscate(fields.projectId))
            .setApplicationId(SecurityUtils.deobfuscate(fields.appId))
            .setApiKey(SecurityUtils.deobfuscate(fields.apiKey))
            .setGcmSenderId(SecurityUtils.deobfuscate(fields.messagingSenderId))
            .setStorageBucket(SecurityUtils.deobfuscate(fields.storageBucket))
            .build()
    }
}
