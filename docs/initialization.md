# Initialization

This SDK is headless: there's no offerwall, no premium widget, no in-app notification popups.
Every network call returns the raw API response body as a JSON `String` (or throws), untouched, so
you parse it with whatever you like (`org.json`, Gson, Moshi, kotlinx.serialization) and build your
own UI (or none at all) on top of it. The integration flow is simple: `init` → `loginUser` →
everything else.

Every network-backed method has a `suspend fun` (call from a coroutine scope) and a Java-friendly
callback overload (`TyradsCallback` / `TyradsResultCallback<T>`).
Kotlin snippets below use the suspend form.

### 1. Initialize

Call `init` once, as early as possible in your app's lifecycle. It only stores credentials,
restores a previous session from disk if present, and prepares the HTTP client. No network call
happens here.

```kotlin
import com.tyrads.sdk.userbase.TyradsUserBase

TyradsUserBase.init(
    context = applicationContext,
    apiKey = "YOUR_API_KEY",
    apiSecret = "YOUR_API_SECRET",
    encKey = "YOUR_32_BYTE_ENC_KEY", // optional, enables AES-256-GCM secure mode
    debugMode = false, // optional, true logs full request/response traffic via Logcat
    interceptors = emptyList(), // optional, extra OkHttp interceptors (e.g. ChuckerInterceptor)
)
```

`interceptors` are added to the SDK's own OkHttp client, after its auth headers, so a network
inspector like [Chucker](https://github.com/ChuckerTeam/chucker) sees the exact requests the SDK
sends. Use it in debug/QA builds only.

A previous login is restored automatically on relaunch (`init()` reads any persisted
`userId`/`token` back into `getSession()`), so no explicit `restore()` call is needed.

### 2. Login

Logging in is mandatory and must be called after `init`. This is the call that actually hits the
network (`POST initialize`) and opens the session. The response's `accountInfo.publisherUserId`
becomes the `X-User-ID` header on every call after this.

```kotlin
val response: String = TyradsUserBase.loginUser("my_unique_user_123") // raw JSON body
```

#### Advanced login (attribution + user info)

```kotlin
import com.tyrads.sdk.userbase.models.TyradsInitOptions
import com.tyrads.sdk.userbase.models.TyradsMediaSourceInfo
import com.tyrads.sdk.userbase.models.TyradsUserInfo

TyradsUserBase.loginUser(
    userId = "my_unique_user_123",
    options = TyradsInitOptions(
        engagementId = 123,
        placementId = 456,
        mediaSourceInfo = TyradsMediaSourceInfo(
            mediaSourceName = "Facebook",
            mediaCampaignName = "Summer2023Promo",
            mediaSourceId = "FB001",
            mediaSubSourceId = "FB001_Stories",
            incentivized = false,
            mediaAdsetName = "YoungAdults25-34",
            mediaAdsetId = "AD001",
            mediaCreativeName = "SummerSale_Video",
            mediaCreativeId = "CR001",
            sub1 = "ReferralCode123",
            sub2 = "OrganicInstall",
            sub3 = "HighValueUser",
            sub4 = "FirstTimeUser",
            sub5 = "AndroidDevice",
        ),
        userInfo = TyradsUserInfo(
            email = "user@example.com",
            phoneNumber = "001234567890", // replace leading + with 00
            userGroup = "High purchase user",
            age = 28,
            gender = 1, // 1 = male, 2 = female
        ),
    ),
)
```

`userGroup` is a free-form string. A plain label works, and so does a JSON string for richer
segmentation, for example:

```kotlin
userGroup = "{\"promo_affinity_ratio\":0.42,\"dynamic_payer_segment\":\"NonPayer\",\"activity_segment\":\"Tier 3\"}"
```

#### `TyradsInitOptions` reference

| Parameter | Type | Description |
|---|---|---|
| `engagementId` | `Int?` | Identifies the engagement placement. |
| `placementId` | `Int?` | Differentiates coin placements within your app. Also used by `getOfferwallUrl()`. |
| `mediaSourceInfo` | `TyradsMediaSourceInfo?` | Attribution data. |
| `userInfo` | `TyradsUserInfo?` | User profile metadata. |

Note: `sub1`, `sub2`, `sub5` are not returned on postback; `sub3` and `sub4` are.

#### `loginUser` response

The raw `POST initialize` body, for example:

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "newRegisteredUser": false,
    "newRegisteredDevice": false,
    "accountInfo": { "id": 47152543, "publisherUserId": "my_unique_user_123" },
    "appInfo": { "headerColor": "#000F1E", "mainColor": "#b32da7", "premiumColor": "#7715A6" },
    "token": "eyJhbGciOi..."
  }
}
```

The SDK reads `data.accountInfo.publisherUserId` (sent as `X-User-ID` from then on) and
`data.token` (returned by `getSession()`) for itself. `appInfo`'s colors are meant for UI theming,
safe to ignore if you don't need them.

### 3. Campaigns

All three return the raw response body as a JSON `String`:

```kotlin
val campaigns: String = TyradsUserBase.getCampaigns()
// -> GET campaigns?mode=userbase: { "code": 200, "data": [ Campaign, ... ], "meta": {...}, "message": "OK" }

val detail: String = TyradsUserBase.getCampaignDetail("4785")
// -> GET campaigns/:id?mode=userbase: { "code": 200, "data": Campaign, "message": "OK" }

val activated: String = TyradsUserBase.getActivatedCampaigns()
// -> GET campaigns/activated: { "data": [ { "groupName", "availableCurrencies", "campaigns": [...] } ], "message": "OK" }

val summary: Int = TyradsUserBase.getActivatedSummary()
// -> GET campaigns/activated/summary, unwrapped to data.activeCampaignCount
```

Parse it with whatever you prefer, for example with the platform's `org.json`:

```kotlin
val firstId = JSONObject(campaigns).getJSONArray("data").getJSONObject(0).getInt("campaignId")
```

The `Campaign` object (fields, events, payout maps, deadlines, statuses) is documented field by
field in the User Base API reference. `campaigns/:id` returns the exact same shape as one list
item.

{% hint style="warning" %}
Always check `errorCode` in the body too. Some errors come back with HTTP 200 and the error code
only in the body, and a 200 without `data` means no offers are available right now.
{% endhint %}

### 4. Activate a Campaign

```kotlin
val result: String = TyradsUserBase.activateCampaign("4785")
// -> POST campaigns/:id/activate: { "code": 200, "data": { "isCampaignActivated": true }, ... }
```

Returns the raw JSON body. To send the user onward, open the campaign's `tracking.clickUrl`
(from the campaign JSON you already fetched) yourself with `Intent(Intent.ACTION_VIEW,
Uri.parse(url))`, a `CustomTabsIntent`, or an in-app `WebView`. This SDK never opens anything on
its own.

### 5. Offerwall URL (no webview shown)

Builds the offerwall URL and hands it back as a plain `String` instead of rendering it. Sync, no
network call:

```kotlin
import com.tyrads.sdk.userbase.constants.TyradsDeepRoutes
import com.tyrads.sdk.userbase.models.TyradsOfferwallUrlOptions

val url = TyradsUserBase.getOfferwallUrl(
    TyradsOfferwallUrlOptions(
        route = TyradsDeepRoutes.SETTINGS, // optional, omit for the main offerwall
        campaignId = "42",                 // optional
    ),
)
```

See [Deeplinking Routes](initialization/deeplinking-routes.md) for the full route reference.
Throws `IllegalStateException` if there's no active session (call `loginUser` first).

### 6. Currency Sale / Engagement Data

```kotlin
val engagement: String? = TyradsUserBase.getEngagement()
// -> GET account/engagement, unwrapped to data.CurrencySales
```

`null` whenever there's no currency sale currently active. Otherwise the raw `CurrencySales` JSON:
`{ name, multiplier, bannerUrl, dateStart, dateEnd, remainingTimeSeconds }`. See also
`limitedTimeEvents` in `getActivatedCampaigns()`.

### 7. Manual Tracking

```kotlin
import com.tyrads.sdk.userbase.constants.TyradsActivity

TyradsUserBase.trackActivity(TyradsActivity.OPENED)
TyradsUserBase.trackActivity(TyradsActivity.CLOSED)
TyradsUserBase.trackActivity("any-custom-string")
```

`TyradsActivity` constants: `INITIALIZED`, `PROFILE_UPDATED`, `OPENED`,
`TARGETED_CAMPAIGN_SHOWN`, `TARGETED_CAMPAIGN_DETAIL_SHOWN`, `CAMPAIGN_ACTIVATED`,
`CAMPAIGN_ACTIVATED_RETRY`, `SUPPORT_TICKET_SHOWN`, `CLOSED`, or pass any custom string.
`trackActivity(TyradsActivity.INITIALIZED)` is already fired automatically on a successful
`loginUser()` call. Nothing else is tracked automatically, so call this yourself at
whatever points matter for your integration.

### 8. Session & Logout

```kotlin
TyradsUserBase.getSession()
// -> TyradsSession(userId, token, isLoginSuccessful, currentLanguage), local state, no network

TyradsUserBase.logoutUser()
// clears the local session (userId/token). Credentials from init() are kept. Call
// loginUser() again to re-authenticate.
```

Both are synchronous, no coroutine/callback needed.

### 9. Language

```kotlin
TyradsUserBase.changeLanguage("id-ID") // Indonesian
```

Sets the `lang` query param used by campaign endpoints. Format is `{lang}-{COUNTRY}` (e.g.
`en-US`, `pt-BR`, `id-ID`). Defaults to whatever the device's locale resolves to (`en-US` on
error). There's no bundled UI to translate, so this only affects what language campaign content
comes back in.

## Notes

* `init` must be called before any other SDK method.
* `loginUser` must be called before any campaign/offerwall/tracking method.
* Every network-backed method is a `suspend fun` (call from a coroutine scope), or use the
  matching `TyradsCallback`/`TyradsResultCallback<T>` overload from Java (results are
  `TyradsResultCallback<String>` for the raw-JSON calls).
  `getSession()` and `changeLanguage()` are synchronous local-state operations either way.
