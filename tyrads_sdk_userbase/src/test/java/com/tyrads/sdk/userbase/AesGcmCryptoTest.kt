package com.tyrads.sdk.userbase

import android.util.Base64
import com.tyrads.sdk.userbase.crypto.AesGcmCrypto
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * `android.util.Base64` is a framework stub in local unit tests, so it's redirected to the JDK's
 * encoder here — everything else (the cipher, the content/tag split) is the real implementation.
 */
class AesGcmCryptoTest {

    private val key = "test-only-32-byte-encryption-key" // exactly 32 UTF-8 bytes

    @Before
    fun redirectAndroidBase64ToJdk() {
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg())
        }
    }

    @After
    fun releaseBase64() {
        unmockkStatic(Base64::class)
    }

    @Test
    fun `key must be exactly 32 bytes`() {
        assertEquals(32, key.toByteArray(Charsets.UTF_8).size)

        assertThrows(IllegalArgumentException::class.java) {
            AesGcmCrypto.encrypt("payload", "too-short")
        }
        assertThrows(IllegalArgumentException::class.java) {
            AesGcmCrypto.encrypt("payload", key + "extra")
        }
    }

    @Test
    fun `envelope decrypts back to the original payload`() {
        val plainText = """{"publisherUserId":"user_1","platform":"Android"}"""

        val envelope = AesGcmCrypto.encrypt(plainText, key)

        assertEquals(plainText, decrypt(envelope.`val`, envelope.vec, envelope.tag, key))
    }

    @Test
    fun `envelope uses a 12-byte iv and a 16-byte tag`() {
        val envelope = AesGcmCrypto.encrypt("payload", key)

        assertEquals(12, decodeBase64(envelope.vec).size)
        assertEquals(16, decodeBase64(envelope.tag).size)
    }

    @Test
    fun `every call uses a fresh iv`() {
        val first = AesGcmCrypto.encrypt("payload", key)
        val second = AesGcmCrypto.encrypt("payload", key)

        assertNotEquals(first.vec, second.vec)
        assertNotEquals(first.`val`, second.`val`)
    }

    @Test
    fun `unicode payloads survive the round trip`() {
        val plainText = """{"userGroup":"Pengguna Café ☕ 日本語"}"""

        val envelope = AesGcmCrypto.encrypt(plainText, key)

        assertEquals(plainText, decrypt(envelope.`val`, envelope.vec, envelope.tag, key))
    }

    @Test
    fun `tampering with the ciphertext fails authentication`() {
        val envelope = AesGcmCrypto.encrypt("payload", key)
        val tampered = decodeBase64(envelope.`val`).also { it[0] = (it[0] + 1).toByte() }

        assertThrows(javax.crypto.AEADBadTagException::class.java) {
            decryptRaw(tampered, decodeBase64(envelope.vec), decodeBase64(envelope.tag), key)
        }
    }

    private fun decodeBase64(value: String): ByteArray = java.util.Base64.getDecoder().decode(value)

    private fun decrypt(content: String, iv: String, tag: String, key: String): String =
        decryptRaw(decodeBase64(content), decodeBase64(iv), decodeBase64(tag), key)

    /** GCM expects ciphertext and tag concatenated — the envelope splits them, so rejoin to verify. */
    private fun decryptRaw(content: ByteArray, iv: ByteArray, tag: ByteArray, key: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES"),
            GCMParameterSpec(128, iv),
        )
        return String(cipher.doFinal(content + tag), Charsets.UTF_8)
    }
}
