# TyrAds SDK Android User Base

[![](https://jitpack.io/v/tyrads-com/tyrads-sdk-android-user-base.svg)](https://jitpack.io/#tyrads-com/tyrads-sdk-android-user-base)

Headless, API-only TyrAds SDK for Android. No bundled offerwall UI. Every call returns typed data
so you build your own UI (or none at all) on top of it: login, campaigns, activity tracking, and a
client-side offerwall URL builder.

## Requirements

* Android API 24+ (`minSdk`)
* Kotlin 1.9+ / AGP 8.x+
* An API key + API secret from the Tyrads Dashboard

## Installation

### Gradle

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

## Quick Start

### 1. Initialize

Call `init` once, as early as possible in your app's lifecycle.

```kotlin
import com.tyrads.sdk.userbase.TyradsUserBase

TyradsUserBase.init(context, apiKey = "YOUR_API_KEY", apiSecret = "YOUR_API_SECRET")
```

### 2. Login

```kotlin
val session = TyradsUserBase.loginUser("user_123")
```

Push notifications need nothing from you: the SDK fetches its own FCM token internally and sends
it on login.

### 3. Fetch data

Network calls return the raw API response body as a JSON `String`, untouched. Parse it with
whatever you like:

```kotlin
val campaigns: String = TyradsUserBase.getCampaigns()
val firstId = JSONObject(campaigns).getJSONArray("data").getJSONObject(0).getInt("campaignId")

val detail: String = TyradsUserBase.getCampaignDetail(firstId.toString())
val activated: String = TyradsUserBase.getActivatedCampaigns()
TyradsUserBase.activateCampaign(firstId.toString())
```

Every method has a `suspend fun` variant and a callback overload
(`TyradsCallback`/`TyradsResultCallback<T>`) for Java callers.

## API

* `init(context, apiKey, apiSecret, encKey?, debugMode?, interceptors?)`: stores credentials, prepares the HTTP
  client. No network call.
* `loginUser(userId, options?)`: `POST initialize`, opens the session and sends the SDK-managed FCM token. Raw JSON.
* `logoutUser()`: clears the local session. Credentials from `init()` are kept.
* `getSession()`: local session state. No network call.
* `changeLanguage(lang)`: sets the `lang` query param used by campaign endpoints.
* `getCampaigns()`: recommended/targeted campaigns. Raw JSON.
* `getCampaignDetail(campaignId)`: raw JSON.
* `getActivatedCampaigns()`: raw JSON.
* `getActivatedSummary()`: `Int`, the active campaign count.
* `getEngagement()`: currency-sale info, raw `CurrencySales` JSON or `null`.
* `activateCampaign(campaignId)`: raw JSON.
* `trackActivity(activity)`: use `TyradsActivity` constants or any custom string.
* `getOfferwallUrl(options?)`: builds the same URL a native offerwall would open in a webview,
  returned as a plain `String` instead of rendered. Optional `route` (`TyradsDeepRoutes.SUPPORT`/
  `SETTINGS`). Requires an active session.

Need the full reference (advanced `loginUser` options, device data fields, push notifications)?
Ask your TyrAds contact for GitBook access to the full docs. See also the [Changelog](CHANGELOG.md).

## Example app

`example/` is a small Android app demonstrating every SDK method against a real backend, with a
button per call and a JSON viewer for the response. It installs the SDK from JitPack, the same way
a publisher would. See
[example/README.md](example/README.md) to run it.
