package com.tyrads.sdk.userbase

import android.content.Context
import android.content.SharedPreferences
import com.tyrads.sdk.userbase.session.SessionStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionStoreTest {

    private lateinit var stored: MutableMap<String, String?>
    private lateinit var store: SessionStore

    @Before
    fun setUp() {
        stored = mutableMapOf()
        store = SessionStore(fakeContext(stored))
    }

    @Test
    fun `credentials persist and secure mode follows the encryption key`() {
        store.setCredentials("key", "secret", null)

        assertEquals("key", store.apiKey)
        assertEquals("secret", store.apiSecret)
        assertFalse(store.isSecure)

        store.setCredentials("key", "secret", "  ")
        assertFalse("a blank key must not enable secure mode", store.isSecure)

        store.setCredentials("key", "secret", "test-only-32-byte-encryption-key")
        assertTrue(store.isSecure)
    }

    @Test
    fun `login state requires both a user id and a token`() {
        assertFalse(store.isLoggedIn)

        store.setUser("user_1", null)
        assertFalse(store.isLoggedIn)

        store.setUser("user_1", "token")
        assertTrue(store.isLoggedIn)
    }

    @Test
    fun `clearSession drops the session but keeps credentials`() {
        store.setCredentials("key", "secret", "test-only-32-byte-encryption-key")
        store.setUser("user_1", "token")

        store.clearSession()

        assertEquals("", store.userId)
        assertNull(store.token)
        assertFalse(store.isLoggedIn)
        assertEquals("key", store.apiKey)
        assertEquals("secret", store.apiSecret)
        assertTrue(store.isSecure)
    }

    @Test
    fun `restore rehydrates a previous session from disk`() {
        store.setCredentials("key", "secret", "enc")
        store.setUser("user_1", "token")
        store.setLanguage("id-ID")

        val reopened = SessionStore(fakeContext(stored))
        assertFalse("nothing is read until restore() runs", reopened.isLoggedIn)

        reopened.restore()

        assertEquals("key", reopened.apiKey)
        assertEquals("secret", reopened.apiSecret)
        assertEquals("enc", reopened.encKey)
        assertEquals("user_1", reopened.userId)
        assertEquals("token", reopened.token)
        assertEquals("id-ID", reopened.currentLanguage)
        assertTrue(reopened.isLoggedIn)
    }

    @Test
    fun `language defaults to en-US`() {
        assertEquals("en-US", store.currentLanguage)

        store.setLanguage("pt-BR")
        assertEquals("pt-BR", store.currentLanguage)
    }

    @Test
    fun `custom ad id is generated once and then reused`() {
        var generated = 0
        val first = store.getOrCreateCustomAdId { generated++; "uuid-$generated" }
        val second = store.getOrCreateCustomAdId { generated++; "uuid-$generated" }

        assertEquals("uuid-1", first)
        assertEquals("uuid-1", second)
        assertEquals(1, generated)

        val reopened = SessionStore(fakeContext(stored))
        assertEquals("uuid-1", reopened.getOrCreateCustomAdId { "should-not-be-called" })
    }

    /** In-memory stand-in for SharedPreferences, shared across instances via [backing]. */
    private fun fakeContext(backing: MutableMap<String, String?>): Context {
        val editor = mockk<SharedPreferences.Editor>()
        val prefs = mockk<SharedPreferences>()
        val context = mockk<Context>()

        val putKey = slot<String>()
        val putValue = slot<String?>()
        every { editor.putString(capture(putKey), captureNullable(putValue)) } answers {
            backing[putKey.captured] = if (putValue.isCaptured) putValue.captured else null
            editor
        }
        val removeKey = slot<String>()
        every { editor.remove(capture(removeKey)) } answers {
            backing.remove(removeKey.captured)
            editor
        }
        every { editor.apply() } returns Unit
        every { prefs.edit() } returns editor

        val getKey = slot<String>()
        val getDefault = slot<String?>()
        every { prefs.getString(capture(getKey), captureNullable(getDefault)) } answers {
            backing[getKey.captured] ?: if (getDefault.isCaptured) getDefault.captured else null
        }

        every { context.applicationContext } returns context
        every { context.getSharedPreferences(any(), any()) } returns prefs
        return context
    }
}
