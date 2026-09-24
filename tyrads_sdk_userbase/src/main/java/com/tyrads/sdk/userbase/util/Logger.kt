package com.tyrads.sdk.userbase.util

import android.util.Log

internal object Logger {
    private const val TAG = "TyradsUserBase"

    @Volatile
    var debugMode: Boolean = false

    fun d(message: String) {
        if (debugMode) Log.d(TAG, message)
    }

    fun w(message: String, throwable: Throwable? = null) {
        if (debugMode) Log.w(TAG, message, throwable)
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (debugMode) Log.e(TAG, message, throwable)
    }
}
