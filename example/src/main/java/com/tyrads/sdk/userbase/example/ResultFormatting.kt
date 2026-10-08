package com.tyrads.sdk.userbase.example

import com.tyrads.sdk.userbase.network.TyradsHttpError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val prettyJson = Json { prettyPrint = true; encodeDefaults = true }

/** The SDK hands back raw JSON strings; this only pretty-prints them for display. */
fun formatSuccess(rawJson: String?): JsonResult =
    rawJson?.let { formatSuccessJson(prettyJson.parseToJsonElement(it)) }
        ?: JsonResult(ok = true, payload = "null")

fun formatSuccessJson(element: JsonElement): JsonResult =
    JsonResult(ok = true, payload = prettyJson.encodeToString(JsonElement.serializer(), element))

fun formatSuccessUrl(url: String): JsonResult =
    formatSuccessJson(buildJsonObject { put("url", url) })

/** Mirrors the RN example's `error?.data ?? error?.message`: prefers the raw server body. */
fun formatError(t: Throwable): JsonResult {
    val element: JsonElement = when (t) {
        is TyradsHttpError.Server -> t.body
            ?.let { runCatching { prettyJson.parseToJsonElement(it) }.getOrNull() }
            ?: JsonPrimitive(t.message ?: "Server error")
        else -> JsonPrimitive(t.message ?: "Unknown error")
    }
    return JsonResult(ok = false, payload = prettyJson.encodeToString(JsonElement.serializer(), element))
}
