package com.tyrads.sdk.userbase.push

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle

/**
 * Invisible trampoline that shows the system POST_NOTIFICATIONS prompt and finishes. A library can't
 * request runtime permissions without an Activity of its own. The full tyrads-sdk-android does the same.
 */
internal class NotificationPermissionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && savedInstanceState == null) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE)
        } else {
            finish()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        finish()
    }

    private companion object {
        const val REQUEST_CODE = 0x7a01
    }
}
