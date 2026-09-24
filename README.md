# TyrAds SDK Android — User Base

Headless, API-only TyrAds SDK for Android. No bundled offerwall UI — every call returns typed data
so you build your own UI (or none at all) on top of it: login, campaigns, activity tracking, and a
client-side offerwall URL builder.

Full documentation: [docs/SUMMARY.md](docs/SUMMARY.md).

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

### 3. Fetch data

```kotlin
val campaigns = TyradsUserBase.getCampaigns()
val detail = TyradsUserBase.getCampaignDetail(campaigns.first().campaignId.toString())
val activated = TyradsUserBase.getActivatedCampaigns()

TyradsUserBase.activateCampaign(campaigns.first().campaignId.toString())
```

See [docs/initialization.md](docs/initialization.md) for the full flow, including advanced login
options (attribution, user info), the offerwall-URL builder, tracking, and logout.

Every method has a `suspend fun` variant and a callback overload
(`TyradsCallback`/`TyradsResultCallback<T>`/`TyradsLoginCallback`) for Java callers.

## API

* `init(context, apiKey, apiSecret, encKey?, debugMode?)` — stores credentials, prepares the HTTP
  client. No network call.
* `loginUser(userId, options?)` — `POST initialize`, opens the session.
* `logoutUser()` — clears the local session. Credentials from `init()` are kept.
* `getSession()` — local session state. No network call.
* `changeLanguage(lang)` — sets the `lang` query param used by campaign endpoints.
* `getCampaigns()` — recommended/targeted campaigns.
* `getCampaignDetail(campaignId)`
* `getActivatedCampaigns()`
* `getActivatedSummary()`
* `getEngagement()` — currency-sale info.
* `activateCampaign(campaignId)`
* `trackActivity(activity)` — use `TyradsActivity` constants or any custom string.
* `getOfferwallUrl(options?)` — builds the same URL a native offerwall would open in a webview,
  returned as a plain `String` instead of rendered. Requires an active session.

## Docs

* [Prerequisite](docs/prerequisite.md)
* [Installation](docs/installation.md)
* [Initialization](docs/initialization.md)
  * [Deeplinking Routes](docs/initialization/deeplinking-routes.md)
  * [Device Data](docs/initialization/device-data.md)
  * [Push Notifications](docs/initialization/push-notifications.md)
* [Obtaining Advertising ID's](docs/obtaining-advertising-ids.md)
* [Changelog](CHANGELOG.md)

## Example app

`example/` is a small Android app demonstrating every SDK method against a real backend, with a
button per call and a JSON viewer for the response. See
[example/README.md](example/README.md) to run it.
