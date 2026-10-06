# Installation

Install via [JitPack](https://jitpack.io):

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("com.github.tyrads-com:tyrads-sdk-android-user-base:v1.0.0")
}
```

The version is the release tag (e.g. `v1.0.0`). See the [Changelog](../CHANGELOG.md) for available
releases.

## Dependencies

This SDK talks to the TyrAds REST API directly over HTTPS via OkHttp+Retrofit. A handful of
small, standard Android/Google libraries come along transitively:

| Library | Used for |
|---|---|
| `com.squareup.retrofit2:retrofit` + `com.squareup.okhttp3:okhttp` | HTTP client |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` + `com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter` | JSON (de)serialization |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `suspend fun` API surface |
| `com.google.android.gms:play-services-ads-identifier` | GAID (Google Advertising ID) for attribution — see [Obtaining Advertising ID's](obtaining-advertising-ids.md) |
| `com.google.android.gms:play-services-appset` | Google App Set ID (part of the device-data payload) |
| `com.scottyab:rootbeer-lib` | Root detection in device data |
| `com.google.firebase:firebase-messaging` | FCM push token, fetched internally and scoped to TyrAds' own Firebase project. Your app needs no Firebase setup, see [Push Notifications](initialization/push-notifications.md) |
| `android.os.Build` / `PackageManager` / `TelephonyManager` / `ConnectivityManager` (platform APIs, no extra library) | Device identity, network, telephony info sent on login |
| `javax.crypto.Cipher` (standard JDK, AES/GCM — no extra library) | AES-256-GCM payload encryption when `encKey` is set |
| `android.content.SharedPreferences` (platform API, no extra library) | Persisting session/credentials on-device |

No separate manual linking step is required — all of the above come in transitively with this
SDK's own Gradle dependency, same as any other Android library.

**Required manifest permissions** (declared by this SDK, merged automatically into your app):

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="com.google.android.gms.permission.AD_ID" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

`READ_PHONE_STATE` is only used for `Build.getSerial()` on API 26–28 (capped to `UNKNOWN` on API
29+ regardless of the permission) — the host app still controls whether it's actually granted at
runtime.
