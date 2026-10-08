package com.tyrads.sdk.userbase.callbacks

/** Java-friendly callback for calls that return no value. */
interface TyradsCallback {
    fun onSuccess()
    fun onFailure(error: String)
}

/** Java-friendly callback for calls that return a value. */
interface TyradsResultCallback<T> {
    fun onSuccess(result: T)
    fun onFailure(error: String)
}

