package com.tyrads.sdk.userbase.network

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

/** The single JSON config used for every request body and response, shared so tests exercise it too. */
@OptIn(ExperimentalSerializationApi::class)
internal val TyradsJson: Json = Json {
    ignoreUnknownKeys = true
    // Nulls stay omitted, but non-null defaults must still be sent: the device-data payload's
    // behavioral counters are all `0` and the backend expects the keys to be present.
    explicitNulls = false
    encodeDefaults = true
    coerceInputValues = true
}
