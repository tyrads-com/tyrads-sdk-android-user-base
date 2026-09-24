package com.tyrads.sdk.userbase.crypto

import android.util.Base64
import com.tyrads.sdk.userbase.models.TyradsEncryptedEnvelope
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM "secure mode" payload encryption. Wire format `{val, vec, tag}` (all Base64,
 * NO_WRAP) matches the backend contract shared with the RN/iOS/native-Android SDKs.
 */
internal object AesGcmCrypto {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12
    const val REQUIRED_KEY_LENGTH_BYTES = 32

    fun encrypt(plainText: String, key: String): TyradsEncryptedEnvelope {
        val keyBytes = key.toByteArray(Charsets.UTF_8)
        require(keyBytes.size == REQUIRED_KEY_LENGTH_BYTES) {
            "encKey must be exactly $REQUIRED_KEY_LENGTH_BYTES UTF-8 bytes, got ${keyBytes.size}"
        }

        val iv = ByteArray(IV_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(TAG_LENGTH_BITS, iv))

        val cipherTextWithTag = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val tagLengthBytes = TAG_LENGTH_BITS / 8
        val content = cipherTextWithTag.copyOfRange(0, cipherTextWithTag.size - tagLengthBytes)
        val tag = cipherTextWithTag.copyOfRange(cipherTextWithTag.size - tagLengthBytes, cipherTextWithTag.size)

        return TyradsEncryptedEnvelope(
            `val` = Base64.encodeToString(content, Base64.NO_WRAP),
            vec = Base64.encodeToString(iv, Base64.NO_WRAP),
            tag = Base64.encodeToString(tag, Base64.NO_WRAP),
        )
    }
}
