package com.tyrads.sdk.userbase.models

import kotlinx.serialization.Serializable

@Serializable
internal data class TrackActivityRequest(
    val activity: String,
)
