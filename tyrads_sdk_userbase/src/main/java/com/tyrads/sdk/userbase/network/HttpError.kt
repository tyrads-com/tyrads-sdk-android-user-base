package com.tyrads.sdk.userbase.network

/** Normalized error thrown by every SDK network call, mirroring the RN SDK's `HttpError` shape. */
sealed class TyradsHttpError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Network(cause: Throwable? = null) : TyradsHttpError("Network error — no response received", cause)
    class Timeout(cause: Throwable? = null) : TyradsHttpError("Request timed out", cause)
    class Server(val status: Int, val body: String?) : TyradsHttpError("Server error: HTTP $status")
    class Unknown(cause: Throwable? = null) : TyradsHttpError(cause?.message ?: "Unknown error", cause)
}
