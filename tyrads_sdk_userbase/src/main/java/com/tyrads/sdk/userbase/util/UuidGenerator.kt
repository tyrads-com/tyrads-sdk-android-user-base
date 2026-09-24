package com.tyrads.sdk.userbase.util

import java.util.UUID

/** Fallback client-generated identifier used when GAID is unavailable/limited (identifierType = OTHER). */
internal object UuidGenerator {
    fun generate(): String = UUID.randomUUID().toString()
}
