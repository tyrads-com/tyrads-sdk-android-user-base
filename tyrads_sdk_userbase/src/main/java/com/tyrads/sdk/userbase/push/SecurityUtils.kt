package com.tyrads.sdk.userbase.push

/**
 * Fixed-salt XOR, same scheme as the other TyrAds SDKs use to keep the embedded Firebase config out
 * of a plain grep of the published artifact. Not real secrecy (the salt ships alongside the data).
 */
internal object SecurityUtils {
    private const val SALT = "android-userbase-fcm-4e7"

    fun deobfuscate(bytes: ByteArray): String {
        val salt = SALT.toByteArray()
        return String(ByteArray(bytes.size) { i -> (bytes[i].toInt() xor salt[i % salt.size].toInt()).toByte() })
    }
}
